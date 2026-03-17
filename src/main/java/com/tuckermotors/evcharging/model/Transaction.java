package com.tuckermotors.evcharging.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a charging transaction from start to stop.
 */
@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @Column(name = "transaction_id", nullable = false, unique = true)
    private String transactionId;

    @Column(name = "station_id", nullable = false)
    private String stationId;

    @Column(name = "id_tag")
    private String idTag;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "stop_time")
    private LocalDateTime stopTime;

    @Column(name = "start_meter_value")
    private Double startMeterValue;

    @Column(name = "stop_meter_value")
    private Double stopMeterValue;

    @Column(name = "total_energy_kwh")
    private Double totalEnergyKwh;

    @Column(name = "stop_reason")
    private String stopReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;

    public enum TransactionStatus {
        ACTIVE, COMPLETED, STOPPED
    }
}
