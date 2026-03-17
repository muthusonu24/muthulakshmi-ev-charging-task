package com.tuckermotors.evcharging.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Captures periodic meter readings during an active charging transaction.
 */
@Entity
@Table(name = "meter_values")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeterValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "station_id", nullable = false)
    private String stationId;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    /** Energy reading in kWh */
    @Column(name = "energy_kwh")
    private Double energyKwh;

    /** Power in kW */
    @Column(name = "power_kw")
    private Double powerKw;

    /** Voltage in V */
    @Column(name = "voltage")
    private Double voltage;

    /** Current in A */
    @Column(name = "current_ampere")
    private Double currentAmpere;
}
