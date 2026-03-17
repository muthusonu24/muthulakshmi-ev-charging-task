package com.tuckermotors.evcharging.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Container for all OCPP message DTOs used as request/response models.
 */
public class OcppMessages {

    // -----------------------------------------------------------------------
    // BootNotification
    // -----------------------------------------------------------------------

    @Data
    public static class BootNotificationRequest {
        @NotBlank(message = "stationId is required")
        private String stationId;
        @NotNull
        private LocalDateTime timestamp;
        private BootPayload payload;

        @Data
        public static class BootPayload {
            private String chargePointVendor;
            private String chargePointModel;
            private String firmwareVersion;
        }
    }

    @Data
    public static class BootNotificationResponse {
        private String status;           // Accepted / Rejected
        private LocalDateTime currentTime;
        private int intervalSeconds;     // Heartbeat interval recommendation
    }

    // -----------------------------------------------------------------------
    // StartTransaction
    // -----------------------------------------------------------------------

    @Data
    public static class StartTransactionRequest {
        @NotBlank
        private String stationId;
        @NotNull
        private LocalDateTime timestamp;
        private StartPayload payload;

        @Data
        public static class StartPayload {
            private String idTag;
            private Double meterStart;   // kWh reading at start
        }
    }

    @Data
    public static class StartTransactionResponse {
        private String transactionId;
        private String idTagStatus;      // Accepted / Blocked / Expired
        private LocalDateTime startTime;
    }

    // -----------------------------------------------------------------------
    // MeterValues
    // -----------------------------------------------------------------------

    @Data
    public static class MeterValuesRequest {
        @NotBlank
        private String stationId;
        @NotBlank
        private String transactionId;
        @NotNull
        private LocalDateTime timestamp;
        private MeterPayload payload;

        @Data
        public static class MeterPayload {
            private Double energy;     // kWh
            private Double power;      // kW
            private Double voltage;    // V
            private Double current;    // A
        }
    }

    @Data
    public static class MeterValuesResponse {
        private String status;
        private String message;
    }

    // -----------------------------------------------------------------------
    // StopTransaction
    // -----------------------------------------------------------------------

    @Data
    public static class StopTransactionRequest {
        @NotBlank
        private String stationId;
        @NotBlank
        private String transactionId;
        @NotNull
        private LocalDateTime timestamp;
        private StopPayload payload;

        @Data
        public static class StopPayload {
            private Double meterStop;   // kWh reading at stop
            private String reason;      // Local / Remote / EVDisconnected / etc.
        }
    }

    @Data
    public static class StopTransactionResponse {
        private String transactionId;
        private String status;
        private Double totalEnergyKwh;
        private long durationSeconds;
    }
}
