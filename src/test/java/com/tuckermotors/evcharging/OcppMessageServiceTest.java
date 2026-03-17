package com.tuckermotors.evcharging;

import com.tuckermotors.evcharging.kafka.OcppKafkaProducer;
import com.tuckermotors.evcharging.model.ChargingStation;
import com.tuckermotors.evcharging.model.OcppMessages.*;
import com.tuckermotors.evcharging.model.Transaction;
import com.tuckermotors.evcharging.repository.ChargingStationRepository;
import com.tuckermotors.evcharging.repository.MeterValueRepository;
import com.tuckermotors.evcharging.repository.TransactionRepository;
import com.tuckermotors.evcharging.service.OcppMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OcppMessageServiceTest {

    @Mock ChargingStationRepository stationRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock MeterValueRepository meterValueRepository;
    @Mock OcppKafkaProducer kafkaProducer;

    @InjectMocks OcppMessageService service;

    private static final String STATION_ID = "EVSE-001";
    private static final String TX_ID = "TXN-ABCD1234";

    // -----------------------------------------------------------------------
    // BootNotification
    // -----------------------------------------------------------------------

    @Test
    void bootNotification_newStation_returnsAccepted() {
        when(stationRepository.findById(STATION_ID)).thenReturn(Optional.empty());
        when(stationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BootNotificationRequest req = new BootNotificationRequest();
        req.setStationId(STATION_ID);
        req.setTimestamp(LocalDateTime.now());
        BootNotificationRequest.BootPayload payload = new BootNotificationRequest.BootPayload();
        payload.setChargePointVendor("ChargePoint");
        payload.setChargePointModel("CP-2000");
        payload.setFirmwareVersion("2.5.1");
        req.setPayload(payload);

        BootNotificationResponse response = service.handleBootNotification(req);

        assertThat(response.getStatus()).isEqualTo("Accepted");
        assertThat(response.getIntervalSeconds()).isEqualTo(300);
        verify(kafkaProducer).publishBootNotification(any(), eq(STATION_ID));
    }

    @Test
    void bootNotification_existingStation_updatesAndAccepts() {
        ChargingStation existing = ChargingStation.builder()
                .stationId(STATION_ID)
                .status(ChargingStation.StationStatus.OFFLINE)
                .build();
        when(stationRepository.findById(STATION_ID)).thenReturn(Optional.of(existing));
        when(stationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BootNotificationRequest req = new BootNotificationRequest();
        req.setStationId(STATION_ID);
        req.setTimestamp(LocalDateTime.now());

        BootNotificationResponse response = service.handleBootNotification(req);

        assertThat(response.getStatus()).isEqualTo("Accepted");
    }

    // -----------------------------------------------------------------------
    // StartTransaction
    // -----------------------------------------------------------------------

    @Test
    void startTransaction_availableStation_returnsTransactionId() {
        ChargingStation station = ChargingStation.builder()
                .stationId(STATION_ID)
                .status(ChargingStation.StationStatus.AVAILABLE)
                .build();
        when(stationRepository.findById(STATION_ID)).thenReturn(Optional.of(station));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(stationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        StartTransactionRequest req = new StartTransactionRequest();
        req.setStationId(STATION_ID);
        req.setTimestamp(LocalDateTime.now());
        StartTransactionRequest.StartPayload payload = new StartTransactionRequest.StartPayload();
        payload.setIdTag("USER-01");
        payload.setMeterStart(0.0);
        req.setPayload(payload);

        StartTransactionResponse response = service.handleStartTransaction(req);

        assertThat(response.getIdTagStatus()).isEqualTo("Accepted");
        assertThat(response.getTransactionId()).startsWith("TXN-");
    }

    @Test
    void startTransaction_offlineStation_returnsBlocked() {
        ChargingStation station = ChargingStation.builder()
                .stationId(STATION_ID)
                .status(ChargingStation.StationStatus.OFFLINE)
                .build();
        when(stationRepository.findById(STATION_ID)).thenReturn(Optional.of(station));

        StartTransactionRequest req = new StartTransactionRequest();
        req.setStationId(STATION_ID);
        req.setTimestamp(LocalDateTime.now());

        StartTransactionResponse response = service.handleStartTransaction(req);

        assertThat(response.getIdTagStatus()).isEqualTo("Blocked");
        verifyNoInteractions(kafkaProducer);
    }

    @Test
    void startTransaction_unknownStation_throwsException() {
        when(stationRepository.findById(STATION_ID)).thenReturn(Optional.empty());

        StartTransactionRequest req = new StartTransactionRequest();
        req.setStationId(STATION_ID);
        req.setTimestamp(LocalDateTime.now());

        assertThatThrownBy(() -> service.handleStartTransaction(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Station not found");
    }

    // -----------------------------------------------------------------------
    // StopTransaction
    // -----------------------------------------------------------------------

    @Test
    void stopTransaction_activeTransaction_calculatesEnergy() {
        Transaction active = Transaction.builder()
                .transactionId(TX_ID)
                .stationId(STATION_ID)
                .startTime(LocalDateTime.now().minusHours(1))
                .startMeterValue(10.0)
                .status(Transaction.TransactionStatus.ACTIVE)
                .build();
        when(transactionRepository.findByTransactionIdAndStatus(TX_ID, Transaction.TransactionStatus.ACTIVE))
                .thenReturn(Optional.of(active));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(stationRepository.findById(STATION_ID)).thenReturn(Optional.empty());

        StopTransactionRequest req = new StopTransactionRequest();
        req.setStationId(STATION_ID);
        req.setTransactionId(TX_ID);
        req.setTimestamp(LocalDateTime.now());
        StopTransactionRequest.StopPayload payload = new StopTransactionRequest.StopPayload();
        payload.setMeterStop(25.5);
        payload.setReason("Local");
        req.setPayload(payload);

        StopTransactionResponse response = service.handleStopTransaction(req);

        assertThat(response.getStatus()).isEqualTo("Accepted");
        assertThat(response.getTotalEnergyKwh()).isEqualTo(15.5);
        assertThat(response.getDurationSeconds()).isGreaterThan(0);
    }

    @Test
    void stopTransaction_notFound_throwsException() {
        when(transactionRepository.findByTransactionIdAndStatus(TX_ID, Transaction.TransactionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        StopTransactionRequest req = new StopTransactionRequest();
        req.setStationId(STATION_ID);
        req.setTransactionId(TX_ID);
        req.setTimestamp(LocalDateTime.now());

        assertThatThrownBy(() -> service.handleStopTransaction(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Active transaction not found");
    }
}
