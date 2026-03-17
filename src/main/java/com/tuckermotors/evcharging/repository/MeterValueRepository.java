package com.tuckermotors.evcharging.repository;

import com.tuckermotors.evcharging.model.MeterValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeterValueRepository extends JpaRepository<MeterValue, Long> {

    List<MeterValue> findByTransactionId(String transactionId);

    List<MeterValue> findByStationId(String stationId);
}
