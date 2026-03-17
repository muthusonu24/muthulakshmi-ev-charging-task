package com.tuckermotors.evcharging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuckermotors.evcharging.model.OcppMessages.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full integration test using an embedded Kafka broker.
 * Tests the complete flow: BootNotification → StartTransaction → MeterValues → StopTransaction.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {
        "ocpp.boot-notification",
        "ocpp.start-transaction",
        "ocpp.meter-values",
        "ocpp.stop-transaction"
})
@DirtiesContext
class EvChargingIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private static final String STATION_ID = "EVSE-IT-001";

    @Test
    void fullChargingSessionFlow() throws Exception {
        // Step 1: BootNotification
        BootNotificationRequest boot = new BootNotificationRequest();
        boot.setStationId(STATION_ID);
        boot.setTimestamp(LocalDateTime.now());
        BootNotificationRequest.BootPayload bootPayload = new BootNotificationRequest.BootPayload();
        bootPayload.setChargePointVendor("TestVendor");
        bootPayload.setChargePointModel("TEST-100");
        bootPayload.setFirmwareVersion("1.0");
        boot.setPayload(bootPayload);

        mockMvc.perform(post("/api/v1/ocpp/boot-notification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(boot)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Accepted"));

        // Step 2: StartTransaction
        StartTransactionRequest start = new StartTransactionRequest();
        start.setStationId(STATION_ID);
        start.setTimestamp(LocalDateTime.now());
        StartTransactionRequest.StartPayload startPayload = new StartTransactionRequest.StartPayload();
        startPayload.setIdTag("USER-TEST-01");
        startPayload.setMeterStart(0.0);
        start.setPayload(startPayload);

        MvcResult startResult = mockMvc.perform(post("/api/v1/ocpp/start-transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(start)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idTagStatus").value("Accepted"))
                .andReturn();

        String responseJson = startResult.getResponse().getContentAsString();
        StartTransactionResponse startResponse = objectMapper.readValue(responseJson, StartTransactionResponse.class);
        String transactionId = startResponse.getTransactionId();
        assertThat(transactionId).startsWith("TXN-");

        // Step 3: MeterValues
        MeterValuesRequest meter = new MeterValuesRequest();
        meter.setStationId(STATION_ID);
        meter.setTransactionId(transactionId);
        meter.setTimestamp(LocalDateTime.now());
        MeterValuesRequest.MeterPayload meterPayload = new MeterValuesRequest.MeterPayload();
        meterPayload.setEnergy(7.5);
        meterPayload.setPower(7.2);
        meterPayload.setVoltage(240.0);
        meterPayload.setCurrent(30.0);
        meter.setPayload(meterPayload);

        mockMvc.perform(post("/api/v1/ocpp/meter-values")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(meter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Accepted"));

        // Step 4: StopTransaction
        StopTransactionRequest stop = new StopTransactionRequest();
        stop.setStationId(STATION_ID);
        stop.setTransactionId(transactionId);
        stop.setTimestamp(LocalDateTime.now());
        StopTransactionRequest.StopPayload stopPayload = new StopTransactionRequest.StopPayload();
        stopPayload.setMeterStop(15.5);
        stopPayload.setReason("Local");
        stop.setPayload(stopPayload);

        mockMvc.perform(post("/api/v1/ocpp/stop-transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stop)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Accepted"))
                .andExpect(jsonPath("$.totalEnergyKwh").value(15.5));

        // Verify station history is queryable
        mockMvc.perform(get("/api/v1/stations/" + STATION_ID + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transactionId").value(transactionId));
    }
}
