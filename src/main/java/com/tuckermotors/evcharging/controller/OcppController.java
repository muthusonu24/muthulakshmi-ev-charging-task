package com.tuckermotors.evcharging.controller;

import com.tuckermotors.evcharging.model.OcppMessages.*;
import com.tuckermotors.evcharging.service.OcppMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * REST controller that exposes OCPP message endpoints.
 * In a production system these would be WebSocket frames; here they are HTTP POST endpoints
 * to keep the implementation straightforward and testable via curl/Postman.
 */
@RestController
@RequestMapping("/api/v1/ocpp")
@RequiredArgsConstructor
@Tag(name = "OCPP Messages", description = "Endpoints that simulate OCPP 1.6 message handling")
public class OcppController {

    private final OcppMessageService ocppMessageService;

    @PostMapping("/boot-notification")
    @Operation(summary = "Handle BootNotification", description = "Called when a charging station comes online")
    public ResponseEntity<BootNotificationResponse> bootNotification(
            @Valid @RequestBody BootNotificationRequest request) {
        return ResponseEntity.ok(ocppMessageService.handleBootNotification(request));
    }

    @PostMapping("/start-transaction")
    @Operation(summary = "Handle StartTransaction", description = "Opens a new charging session")
    public ResponseEntity<StartTransactionResponse> startTransaction(
            @Valid @RequestBody StartTransactionRequest request) {
        return ResponseEntity.ok(ocppMessageService.handleStartTransaction(request));
    }

    @PostMapping("/meter-values")
    @Operation(summary = "Handle MeterValues", description = "Records periodic energy readings during a session")
    public ResponseEntity<MeterValuesResponse> meterValues(
            @Valid @RequestBody MeterValuesRequest request) {
        return ResponseEntity.ok(ocppMessageService.handleMeterValues(request));
    }

    @PostMapping("/stop-transaction")
    @Operation(summary = "Handle StopTransaction", description = "Closes an active charging session")
    public ResponseEntity<StopTransactionResponse> stopTransaction(
            @Valid @RequestBody StopTransactionRequest request) {
        return ResponseEntity.ok(ocppMessageService.handleStopTransaction(request));
    }
}
