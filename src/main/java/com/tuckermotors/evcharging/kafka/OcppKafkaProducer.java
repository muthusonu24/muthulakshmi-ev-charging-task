package com.tuckermotors.evcharging.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class OcppKafkaProducer {

    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.boot-notification}")
    private String bootNotificationTopic;

    @Value("${kafka.topics.start-transaction}")
    private String startTransactionTopic;

    @Value("${kafka.topics.meter-values}")
    private String meterValuesTopic;

    @Value("${kafka.topics.stop-transaction}")
    private String stopTransactionTopic;

    @Value("${spring.kafka.enabled:false}")
    private boolean kafkaEnabled;

    public void publishBootNotification(Object message, String stationId) {
        log.info("BootNotification event for station [{}]", stationId);
    }

    public void publishStartTransaction(Object message, String transactionId) {
        log.info("StartTransaction event for transaction [{}]", transactionId);
    }

    public void publishMeterValues(Object message, String transactionId) {
        log.info("MeterValues event for transaction [{}]", transactionId);
    }

    public void publishStopTransaction(Object message, String transactionId) {
        log.info("StopTransaction event for transaction [{}]", transactionId);
    }
}