package com.tuckermotors.evcharging.repository;

import com.tuckermotors.evcharging.model.ChargingStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChargingStationRepository extends JpaRepository<ChargingStation, String> {

    List<ChargingStation> findByStatus(ChargingStation.StationStatus status);
}
