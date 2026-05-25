# PUF Vault Server

Spring Boot backend handling:

- HTTPS/API
- Authentication
- RSA encryption
- UART communication
- Diagnostics
- Device monitoring

---

# Requirements

- Java 17+
- Maven
- Arduino connected via USB

---

# Configure Serial Port

Edit:

src/main/resources/application.yml

Example:

serial:
  port: /dev/cu.usbmodem1101
  baud: 115200
  timeoutMs: 3000

Find ports on Mac:

ls /dev/cu.*

---

# Run Server

From server directory:

mvn spring-boot:run

Server:

http://localhost:8080

---

# Health Endpoint

GET /api/health

---

# Main API Endpoints

## Authentication

POST /api/login

---

## Services

GET /api/services
POST /api/services
DELETE /api/services/{serviceId}
POST /api/services/{serviceId}/generate
POST /api/services/{serviceId}/rotate

---

## Device

GET /api/device/status
GET /api/device/capability
GET /api/device/diagnostics
GET /api/device/uart
POST /api/device/reconnect
POST /api/device/ping
POST /api/device/wipe

---

# UART Monitoring

The server stores UART transcript history.

Displayed in dashboard:

- timestamp
- sender
- receiver
- message

Example:

Server → Arduino
ADD_SERVICE github.com

Arduino → Server
OK SERVICE_ADDED SLOT=0

---

# Connection Indicators

## Server Connection

Green:

/api/health reachable

## PUF Device Connection

Green:

OK READY

Amber:

NOK POWER_CYCLE_REQUIRED

Red:

Device unreachable

---

# Reconnect Logic

The server automatically:

- reopens serial port
- retries failed UART commands
- detects unplug/replug events

Manual reconnect available in dashboard.