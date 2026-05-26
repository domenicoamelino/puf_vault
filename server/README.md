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
- Arduino generates plaintext password
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