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