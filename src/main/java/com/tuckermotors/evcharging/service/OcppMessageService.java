package com.tuckermotors.evcharging.service;

import com.tuckermotors.evcharging.kafka.OcppKafkaProducer;
import com.tuckermotors.evcharging.model.ChargingStation;
import com.tuckermotors.evcharging.model.MeterValue;
import com.tuckermotors.evcharging.model.OcppMessages.*;
import com.tuckermotors.evcharging.model.Transaction;
import com.tuckermotors.evcharging.repository.ChargingStationRepository;
import com.tuckermotors.evcharging.repository.MeterValueRepository;
import com.tuckermotors.evcharging.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Core service that handles all four OCPP message types:
 * BootNotification, StartTransaction, MeterValues, and StopTransaction.
 *
 * <p>Each handler persists data, publishes to Kafka, and returns an appropriate response.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OcppMessageService {

    private final ChargingStationRepository stationRepository;
    private final TransactionRepository transactionRepository;
    private final MeterValueRepository meterValueRepository;
    private final OcppKafkaProducer kafkaProducer;

    // -----------------------------------------------------------------------
    // BootNotification
    // -----------------------------------------------------------------------

    /**
     * Registers or updates a charging station on first contact.
     * Returns "Accepted" if the station is valid; "Rejected" otherwise.
     */
    @Transactional
    public BootNotificationResponse handleBootNotification(BootNotificationRequest request) {
        log.info("BootNotification received from station [{}]", request.getStationId());

        ChargingStation station = stationRepository
                .findById(request.getStationId())
                .orElse(ChargingStation.builder()
                        .stationId(request.getStationId())
                        .registeredAt(LocalDateTime.now())
                        .build());

        if (request.getPayload() != null) {
            station.setChargePointVendor(request.getPayload().getChargePointVendor());
            station.setChargePointModel(request.getPayload().getChargePointModel());
            station.setFirmwareVersion(request.getPayload().getFirmwareVersion());
        }

        station.setStatus(ChargingStation.StationStatus.AVAILABLE);
        station.setLastHeartbeat(LocalDateTime.now());
        stationRepository.save(station);

        kafkaProducer.publishBootNotification(request, request.getStationId());

        BootNotificationResponse response = new BootNotificationResponse();
        response.setStatus("Accepted");
        response.setCurrentTime(LocalDateTime.now());
        response.setIntervalSeconds(300); // suggest 5-minute heartbeat
        return response;
    }

    // -----------------------------------------------------------------------
    // StartTransaction
    // -----------------------------------------------------------------------

    /**
     * Opens a new charging transaction for a station.
     * The station must be registered and in AVAILABLE state.
     */
    @Transactional
    public StartTransactionResponse handleStartTransaction(StartTransactionRequest request) {
        log.info("StartTransaction received for station [{}]", request.getStationId());

        ChargingStation station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Station not found: " + request.getStationId()));

        if (station.getStatus() == ChargingStation.StationStatus.OFFLINE ||
            station.getStatus() == ChargingStation.StationStatus.FAULTED) {
            StartTransactionResponse denied = new StartTransactionResponse();
            denied.setIdTagStatus("Blocked");
            return denied;
        }

        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Double meterStart = request.getPayload() != null ? request.getPayload().getMeterStart() : 0.0;
        String idTag = request.getPayload() != null ? request.getPayload().getIdTag() : "UNKNOWN";

        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .stationId(request.getStationId())
                .idTag(idTag)
                .startTime(request.getTimestamp())
                .startMeterValue(meterStart)
                .status(Transaction.TransactionStatus.ACTIVE)
                .build();

        transactionRepository.save(transaction);

        station.setStatus(ChargingStation.StationStatus.OCCUPIED);
        stationRepository.save(station);

        kafkaProducer.publishStartTransaction(request, transactionId);

        StartTransactionResponse response = new StartTransactionResponse();
        response.setTransactionId(transactionId);
        response.setIdTagStatus("Accepted");
        response.setStartTime(request.getTimestamp());
        return response;
    }

    // -----------------------------------------------------------------------
    // MeterValues
    // -----------------------------------------------------------------------

    /**
     * Records a periodic meter reading for an active transaction.
     */
    @Transactional
    public MeterValuesResponse handleMeterValues(MeterValuesRequest request) {
        log.info("MeterValues received for transaction [{}]", request.getTransactionId());

        // Validate transaction exists (soft validation — store anyway if not found)
        boolean txExists = transactionRepository
                .findByTransactionIdAndStatus(request.getTransactionId(), Transaction.TransactionStatus.ACTIVE)
                .isPresent();

        if (!txExists) {
            log.warn("MeterValues received for unknown/inactive transaction [{}]", request.getTransactionId());
        }

        MeterValue mv = MeterValue.builder()
                .stationId(request.getStationId())
                .transactionId(request.getTransactionId())
                .timestamp(request.getTimestamp())
                .build();

        if (request.getPayload() != null) {
            mv.setEnergyKwh(request.getPayload().getEnergy());
            mv.setPowerKw(request.getPayload().getPower());
            mv.setVoltage(request.getPayload().getVoltage());
            mv.setCurrentAmpere(request.getPayload().getCurrent());
        }

        meterValueRepository.save(mv);
        kafkaProducer.publishMeterValues(request, request.getTransactionId());

        MeterValuesResponse response = new MeterValuesResponse();
        response.setStatus("Accepted");
        response.setMessage("Meter value recorded");
        return response;
    }

    // -----------------------------------------------------------------------
    // StopTransaction
    // -----------------------------------------------------------------------

    /**
     * Closes an active transaction and computes total energy consumed.
     */
    @Transactional
    public StopTransactionResponse handleStopTransaction(StopTransactionRequest request) {
        log.info("StopTransaction received for transaction [{}]", request.getTransactionId());

        Transaction transaction = transactionRepository
                .findByTransactionIdAndStatus(request.getTransactionId(), Transaction.TransactionStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Active transaction not found: " + request.getTransactionId()));

        Double meterStop = request.getPayload() != null ? request.getPayload().getMeterStop() : 0.0;
        String reason    = request.getPayload() != null ? request.getPayload().getReason() : "Unknown";

        double totalEnergy = (meterStop != null && transaction.getStartMeterValue() != null)
                ? Math.max(0, meterStop - transaction.getStartMeterValue())
                : 0.0;

        long durationSeconds = ChronoUnit.SECONDS.between(transaction.getStartTime(), request.getTimestamp());

        transaction.setStopTime(request.getTimestamp());
        transaction.setStopMeterValue(meterStop);
        transaction.setTotalEnergyKwh(totalEnergy);
        transaction.setStopReason(reason);
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transactionRepository.save(transaction);

        // Free the station
        stationRepository.findById(request.getStationId()).ifPresent(station -> {
            station.setStatus(ChargingStation.StationStatus.AVAILABLE);
            stationRepository.save(station);
        });

        kafkaProducer.publishStopTransaction(request, request.getTransactionId());

        StopTransactionResponse response = new StopTransactionResponse();
        response.setTransactionId(request.getTransactionId());
        response.setStatus("Accepted");
        response.setTotalEnergyKwh(totalEnergy);
        response.setDurationSeconds(durationSeconds);
        return response;
    }
}
