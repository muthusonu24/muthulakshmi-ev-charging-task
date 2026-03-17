package com.tuckermotors.evcharging.controller;

import com.tuckermotors.evcharging.model.ChargingStation;
import com.tuckermotors.evcharging.model.Transaction;
import com.tuckermotors.evcharging.service.ChargingStationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for querying charging stations, transactions, and energy stats.
 */
@RestController
@RequestMapping("/api/v1/stations")
@RequiredArgsConstructor
@Tag(name = "Charging Stations", description = "Query endpoints for stations, transactions, and energy data")
public class ChargingStationController {

    private final ChargingStationQueryService queryService;

    @GetMapping
    @Operation(summary = "Get all charging stations")
    public ResponseEntity<List<ChargingStation>> getAllStations() {
        return ResponseEntity.ok(queryService.getAllStations());
    }

    @GetMapping("/transactions/active")
    @Operation(summary = "Get all active (ongoing) transactions")
    public ResponseEntity<List<Transaction>> getActiveTransactions() {
        return ResponseEntity.ok(queryService.getActiveTransactions());
    }

    @GetMapping("/{stationId}/history")
    @Operation(summary = "Get charging history for a specific station")
    public ResponseEntity<List<Transaction>> getStationHistory(@PathVariable String stationId) {
        return ResponseEntity.ok(queryService.getTransactionHistory(stationId));
    }

    @GetMapping("/energy/last-24h")
    @Operation(summary = "Get total energy consumed in the last 24 hours (kWh)")
    public ResponseEntity<Map<String, Double>> getTotalEnergy24h() {
        Double totalEnergy = queryService.getTotalEnergyLast24Hours();
        return ResponseEntity.ok(Map.of("totalEnergyKwh", totalEnergy));
    }
}
