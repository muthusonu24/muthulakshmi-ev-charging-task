package com.tuckermotors.evcharging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuckermotors.evcharging.model.MeterValue;
import com.tuckermotors.evcharging.model.OcppMessages.MeterValuesRequest;
import com.tuckermotors.evcharging.repository.MeterValueRepository;
import com.tuckermotors.evcharging.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Kafka consumer that processes MeterValues events from the ocpp.meter-values topic.
 *
 * <p>For each consumed message it calculates:
 * <ul>
 *   <li>Total energy consumed for the transaction</li>
 *   <li>Charging duration</li>
 *   <li>Average power</li>
 * </ul>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MeterValuesConsumer {

    private final MeterValueRepository meterValueRepository;
    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${kafka.topics.meter-values}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message) {
        try {
            MeterValuesRequest request = objectMapper.readValue(message, MeterValuesRequest.class);
            log.debug("Consumed MeterValues for transaction [{}]", request.getTransactionId());
            calculateAndLogStats(request.getTransactionId());
        } catch (Exception e) {
            log.error("Error processing MeterValues message: {}", e.getMessage(), e);
        }
    }

    /**
     * Derives aggregate statistics from all stored meter readings for a transaction.
     */
    private void calculateAndLogStats(String transactionId) {
        List<MeterValue> readings = meterValueRepository.findByTransactionId(transactionId);
        if (readings.isEmpty()) return;

        double totalEnergy = readings.stream()
                .mapToDouble(mv -> mv.getEnergyKwh() != null ? mv.getEnergyKwh() : 0.0)
                .max()
                .orElse(0.0);

        double avgPower = readings.stream()
                .mapToDouble(mv -> mv.getPowerKw() != null ? mv.getPowerKw() : 0.0)
                .average()
                .orElse(0.0);

        long durationMinutes = 0;
        if (readings.size() >= 2) {
            var first = readings.get(0).getTimestamp();
            var last = readings.get(readings.size() - 1).getTimestamp();
            durationMinutes = java.time.Duration.between(first, last).toMinutes();
        }

        log.info("Transaction [{}] stats -> totalEnergy: {} kWh | avgPower: {} kW | duration: {} min",
                transactionId,
                String.format("%.2f", totalEnergy),
                String.format("%.2f", avgPower),
                durationMinutes);
    }
}
