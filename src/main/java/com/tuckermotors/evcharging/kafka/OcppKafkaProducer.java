package com.tuckermotors.evcharging.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes OCPP messages to their corresponding Kafka topics.
 * Each message type is routed to a dedicated topic for downstream consumers.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OcppKafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.boot-notification}")
    private String bootNotificationTopic;

    @Value("${kafka.topics.start-transaction}")
    private String startTransactionTopic;

    @Value("${kafka.topics.meter-values}")
    private String meterValuesTopic;

    @Value("${kafka.topics.stop-transaction}")
    private String stopTransactionTopic;

    public void publishBootNotification(Object message, String stationId) {
        publish(bootNotificationTopic, stationId, message);
    }

    public void publishStartTransaction(Object message, String transactionId) {
        publish(startTransactionTopic, transactionId, message);
    }

    public void publishMeterValues(Object message, String transactionId) {
        publish(meterValuesTopic, transactionId, message);
    }

    public void publishStopTransaction(Object message, String transactionId) {
        publish(stopTransactionTopic, transactionId, message);
    }

    private void publish(String topic, String key, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, key, json);
            log.debug("Published to topic [{}] with key [{}]", topic, key);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message for topic [{}]: {}", topic, e.getMessage());
        }
    }
}
