package com.tuckermotors.evcharging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the EV Charging Station Management System (CSMS).
 * Provides OCPP message handling, Kafka integration, and REST APIs.
 */
@SpringBootApplication
@EnableScheduling
public class EvChargingApplication {

    public static void main(String[] args) {
        SpringApplication.run(EvChargingApplication.class, args);
    }
}
