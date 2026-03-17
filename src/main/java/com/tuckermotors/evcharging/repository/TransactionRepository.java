package com.tuckermotors.evcharging.repository;

import com.tuckermotors.evcharging.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    List<Transaction> findByStatus(Transaction.TransactionStatus status);

    List<Transaction> findByStationId(String stationId);

    Optional<Transaction> findByTransactionIdAndStatus(String transactionId, Transaction.TransactionStatus status);

    @Query("SELECT COALESCE(SUM(t.totalEnergyKwh), 0) FROM Transaction t " +
           "WHERE t.stopTime >= :since AND t.status = 'COMPLETED'")
    Double sumEnergyConsumedSince(@Param("since") LocalDateTime since);
}
