# EV Charging Station Management System (CSMS)

A simplified backend for managing EV charging stations, built with **Java 17**, **Spring Boot 2.7**, **Apache Kafka**, and **H2** in-memory database.

---

## Table of Contents
- [Architecture Overview](#architecture-overview)
- [Prerequisites](#prerequisites)
- [Running the Application](#running-the-application)
  - [Option 1: Docker Compose (Recommended)](#option-1-docker-compose-recommended)
  - [Option 2: Local with External Kafka](#option-2-local-with-external-kafka)
- [API Documentation](#api-documentation)
- [Sample Requests](#sample-requests)
- [Assumptions](#assumptions)
- [Suggested Improvements](#suggested-improvements)

---

## Architecture Overview

```
┌──────────────────────────────────────────────────────┐
│                Spring Boot Application                │
│                                                       │
│  ┌─────────────┐   ┌──────────────┐   ┌───────────┐  │
│  │  REST API   │──▶│   Services   │──▶│  H2 DB    │  │
│  │ Controllers │   │ (Business    │   │(JPA/Repos)│  │
│  └─────────────┘   │  Logic)      │   └───────────┘  │
│                    └──────┬───────┘                  │
│                           │ publish                  │
│                    ┌──────▼───────┐                  │
│                    │Kafka Producer│                  │
│                    └──────┬───────┘                  │
└───────────────────────────┼──────────────────────────┘
                            │
              ┌─────────────▼──────────────┐
              │         Apache Kafka        │
              │  ocpp.boot-notification     │
              │  ocpp.start-transaction     │
              │  ocpp.meter-values          │
              │  ocpp.stop-transaction      │
              └─────────────┬──────────────┘
                            │ consume
              ┌─────────────▼──────────────┐
              │    MeterValues Consumer     │
              │  - Total energy consumed    │
              │  - Charging duration        │
              │  - Average power            │
              └────────────────────────────┘
```

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 17+ |
| Maven | 3.8+ |
| Docker + Docker Compose | Latest |

---

## Running the Application

### Option 1: Docker Compose (Recommended)

Starts Zookeeper, Kafka, and the application together:

```bash
docker-compose up --build
```

The application will be available at `http://localhost:8080`.

### Option 2: Local with External Kafka

**Step 1** — Start Kafka (skip if already running):
```bash
# Using Confluent Platform or your own Kafka installation
# Default expected at localhost:9092
```

**Step 2** — Build and run:
```bash
mvn clean package -DskipTests
java -jar target/ev-charging-1.0.0.jar
```

**Step 3** — To run tests (uses embedded Kafka):
```bash
mvn test
```

---

## API Documentation

Interactive Swagger UI is available at:
```
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON spec:
```
http://localhost:8080/api-docs
```

H2 Console (in-memory DB viewer):
```
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:evcharging
Username: sa  |  Password: (empty)
```

---

### OCPP Message Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/ocpp/boot-notification` | Register a station coming online |
| POST | `/api/v1/ocpp/start-transaction` | Begin a charging session |
| POST | `/api/v1/ocpp/meter-values` | Send a periodic meter reading |
| POST | `/api/v1/ocpp/stop-transaction` | End a charging session |

### Query Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/stations` | List all charging stations |
| GET | `/api/v1/stations/transactions/active` | List active (ongoing) sessions |
| GET | `/api/v1/stations/{stationId}/history` | Transaction history for a station |
| GET | `/api/v1/stations/energy/last-24h` | Total energy consumed in last 24 h |

---

## Sample Requests

### 1. BootNotification — Station comes online

```bash
curl -X POST http://localhost:8080/api/v1/ocpp/boot-notification \
  -H "Content-Type: application/json" \
  -d '{
    "stationId": "EVSE-001",
    "timestamp": "2024-01-15T10:30:00",
    "payload": {
      "chargePointVendor": "ChargePoint",
      "chargePointModel": "CP-2000",
      "firmwareVersion": "2.5.1"
    }
  }'
```

**Response:**
```json
{
  "status": "Accepted",
  "currentTime": "2024-01-15T10:30:01",
  "intervalSeconds": 300
}
```

---

### 2. StartTransaction — Begin charging

```bash
curl -X POST http://localhost:8080/api/v1/ocpp/start-transaction \
  -H "Content-Type: application/json" \
  -d '{
    "stationId": "EVSE-001",
    "timestamp": "2024-01-15T10:35:00",
    "payload": {
      "idTag": "USER-CARD-42",
      "meterStart": 0.0
    }
  }'
```

**Response:**
```json
{
  "transactionId": "TXN-A1B2C3D4",
  "idTagStatus": "Accepted",
  "startTime": "2024-01-15T10:35:00"
}
```

---

### 3. MeterValues — Periodic energy reading

```bash
curl -X POST http://localhost:8080/api/v1/ocpp/meter-values \
  -H "Content-Type: application/json" \
  -d '{
    "stationId": "EVSE-001",
    "transactionId": "TXN-A1B2C3D4",
    "timestamp": "2024-01-15T10:40:00",
    "payload": {
      "energy": 15.5,
      "power": 7.2,
      "voltage": 240,
      "current": 30
    }
  }'
```

---

### 4. StopTransaction — End charging

```bash
curl -X POST http://localhost:8080/api/v1/ocpp/stop-transaction \
  -H "Content-Type: application/json" \
  -d '{
    "stationId": "EVSE-001",
    "transactionId": "TXN-A1B2C3D4",
    "timestamp": "2024-01-15T11:35:00",
    "payload": {
      "meterStop": 45.3,
      "reason": "Local"
    }
  }'
```

**Response:**
```json
{
  "transactionId": "TXN-A1B2C3D4",
  "status": "Accepted",
  "totalEnergyKwh": 45.3,
  "durationSeconds": 3600
}
```

---

### 5. Query APIs

```bash
# All stations
curl http://localhost:8080/api/v1/stations

# Active transactions
curl http://localhost:8080/api/v1/stations/transactions/active

# History for EVSE-001
curl http://localhost:8080/api/v1/stations/EVSE-001/history

# Total energy last 24 hours
curl http://localhost:8080/api/v1/stations/energy/last-24h
```

---

## Assumptions

1. **OCPP over HTTP** — The task specifies understanding of OCPP concepts. For simplicity and testability, messages are received as REST (JSON/HTTP) rather than raw OCPP WebSocket frames. In production, a WebSocket layer (e.g. using Spring WebSocket + STOMP or a dedicated OCPP library like `steve`) would handle native OCPP framing.

2. **Embedded Kafka vs. External** — Tests use `spring-kafka-test` embedded Kafka. The application is configured to connect to `localhost:9092` by default but this is overridable via the `KAFKA_BOOTSTRAP_SERVERS` environment variable.

3. **Single connector per station** — The model assumes one active transaction per station at a time. Multi-connector stations would need a connector-level state model.

4. **MeterValues energy field** — Treated as the cumulative kWh reading since transaction start (not a delta per reading). `totalEnergyKwh = meterStop - meterStart`.

5. **idTag validation** — Any non-null idTag is accepted. A real CSMS would validate against an authorization list or OCPP `Authorize` request.

6. **No authentication** — REST endpoints are unsecured. Production systems would use OAuth 2.0 or API keys.

---

## Suggested Improvements

- **Native WebSocket OCPP** — Replace HTTP endpoints with a proper OCPP 1.6/2.0.1 WebSocket handler using the [Steve](https://github.com/steve-community/steve) library or a custom Spring WebSocket endpoint.
- **Multi-connector support** — Add a `Connector` entity linked to `ChargingStation` with its own status/transaction.
- **Authorization service** — Validate RFID/idTags against a local allow-list or remote OCPP `Authorize` requests.
- **Persistent database** — Replace H2 with PostgreSQL for production durability.
- **Kafka partitioning by stationId** — Ensure ordering guarantees per station by using `stationId` as the Kafka partition key.
- **Health/metrics endpoints** — Add Spring Actuator + Prometheus metrics for observability.
- **Rate limiting** — Protect OCPP endpoints from misbehaving stations using a token bucket approach.
- **OCPP 2.0.1** — Upgrade message structures to support newer features (device management, smart charging profiles).
