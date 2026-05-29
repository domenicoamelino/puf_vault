# PUF Vault Server

Spring Boot backend handling:

- HTTPS/API communication
- Authentication
- RSA encryption
- UART communication
- Device diagnostics
- UART monitoring
- Device reconnect logic
- Live dashboard support

---

# Requirements

- Java 17+
- Maven
- Arduino connected over USB

---

# Configure Serial Port

The server detects the Arduino automatically by scanning available serial ports and probing each candidate with the firmware `STATUS` command. This is the default and is suitable for Raspberry Pi paths such as `/dev/ttyACM0` and `/dev/ttyUSB0`.

Edit:
src/main/resources/application.yml

Example:

serial:
  port: auto
  baud: 115200
  timeoutMs: 3000

If you need to prefer a specific port for debugging, set `serial.port` to that path. The server tries that value first, then falls back to auto-detected ports if it does not respond like a PUF Vault Arduino.

---

# Configured Users

## Personal User

Username:
demo

Password:
demo123

Slots:
3

Animation:
disabled

---

## Test User

Username:
test

Password:
test123

Slots:
2

Animation:
enabled

---

# Run Server

From server directory:

mvn spring-boot:run

Server:
http://localhost:8080

---

# Main API Endpoints

## Authentication

POST /api/login

---

## Services

GET /api/services

POST /api/services

When a service is created, the server generates a UART-safe creation nonce such as `20260526T163012Z_9f2c1d3a` and sends it to the device as `ADD_SERVICE <userId> <serviceId> <creationNonce>`. The generated nonce is 25 characters, fitting the firmware field that accepts up to 31 UART-safe characters (`A-Z a-z 0-9 _ - .`). The Arduino stores that nonce in EEPROM and includes it in password derivation, so deleting and recreating the same service ID produces a different password.

DELETE /api/services/{serviceId}

POST /api/services/{serviceId}/generate

POST /api/services/{serviceId}/rotate

---

## Device

GET /api/health

Public server liveness check. This endpoint never calls the Arduino and should remain green as long as Spring Boot is reachable.

GET /api/device/health

Authenticated, non-throwing dashboard health check for the Arduino. It sends a lightweight `STATUS` command, catches serial exceptions, and returns HTTP 200 with `deviceState` values such as `READY`, `POWER_CYCLE_REQUIRED`, or `DISCONNECTED` while the server is alive.

GET /api/device/status

Authenticated manual/debug `STATUS` command. This endpoint may fail if the Arduino is unavailable and should not be used for dashboard keep-alive polling.

GET /api/device/capability

GET /api/device/diagnostics

Authenticated serial diagnostics. The response includes `currentPort`, `availablePorts`, `lastFailure`, `lastFailureAt`, `lastCommand`, and `lastResponse` even when the Arduino is disconnected.

GET /api/device/uart

POST /api/device/reconnect

POST /api/device/ping

POST /api/device/wipe

---

# UART Monitoring

The server stores UART transcript history.

The dashboard displays:
- timestamp
- sender
- receiver
- message

Example:

Server → Arduino
GENERATE_PASSWORD user001 github.com

Arduino → Server
OK PASSWORD xxxxx

---

# Connection Indicators

## Server Connection

Green:
backend reachable

Red:
server unavailable

---

## PUF Device Connection

Green:
device operational

Amber:
connected but requires repower

Red:
device unreachable

---

# RSA Flow

During login:
- browser generates RSA keypair
- public key sent to server
- private key stays local

During password reveal:
- Arduino generates plaintext password from PUF material, user ID, service ID, policy ID, version, and the stored creation nonce
- server encrypts password
- browser decrypts locally

---

# Device Reconnect Logic

The server automatically:
- retries UART operations
- reopens serial port
- detects unplug/replug events

Manual reconnect available from dashboard.

---

# Educational Animation Mode

The test user enables a live communication visualization showing:

- HTTPS/TLS encrypted traffic
- UART serial communication
- PUF password generation
- RSA encryption flow

This mode is hidden for the personal user.

---

# Prototype Limitations

Current simplifications:
- no fuzzy extractor ECC
- no helper data
- UART considered trusted
- no hardware secure enclave
- no secure challenge-response