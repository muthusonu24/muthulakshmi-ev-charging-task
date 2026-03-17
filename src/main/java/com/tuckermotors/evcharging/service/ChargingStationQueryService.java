package com.tuckermotors.evcharging.service;

import com.tuckermotors.evcharging.model.ChargingStation;
import com.tuckermotors.evcharging.model.Transaction;
import com.tuckermotors.evcharging.repository.ChargingStationRepository;
import com.tuckermotors.evcharging.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only service for querying stations, transactions, and energy statistics.
 */
@Service
@RequiredArgsConstructor
public class ChargingStationQueryService {

    private final ChargingStationRepository stationRepository;
    private final TransactionRepository transactionRepository;

    public List<ChargingStation> getAllStations() {
        return stationRepository.findAll();
    }

    public List<Transaction> getActiveTransactions() {
        return transactionRepository.findByStatus(Transaction.TransactionStatus.ACTIVE);
    }

    public List<Transaction> getTransactionHistory(String stationId) {
        return transactionRepository.findByStationId(stationId);
    }

    /**
     * Returns total energy consumed (kWh) across all completed transactions in the last 24 hours.
     */
    public Double getTotalEnergyLast24Hours() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return transactionRepository.sumEnergyConsumedSince(since);
    }
}
