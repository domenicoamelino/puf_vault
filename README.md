# PUF Vault

PUF Vault is a lightweight password vault prototype based on a Physical Unclonable Function (PUF) implemented on an Arduino device.

The system is composed of:

- Web client (HTML/CSS/JavaScript)
- Spring Boot Java server
- Arduino-based PUF device over UART serial communication

The core concept is that passwords are not permanently stored. Instead, they are deterministically regenerated from:

- SRAM startup entropy (PUF material)
- User identity
- Service identifier
- Rotation version
- Per-service creation nonce
- Internal policy information

The generated password is then encrypted using the client RSA public key before being returned to the browser.

---

# Current Architecture

Browser Client
↓ HTTPS/TLS
Spring Boot Server
↓ UART Serial
Arduino PUF Device

---

# Main Features

- Deterministic password generation
- SRAM-based PUF entropy
- RSA browser-side encryption/decryption
- UART monitoring dashboard
- Live communication animation
- Password rotation
- Service deletion
- Device diagnostics
- Device reconnection
- Device health indicators
- Repower detection

---

# Current User Model

Two users are currently configured:

## Personal User

Username:
demo

Password:
demo123

Capabilities:
- 3 service slots
- Standard UI
- No educational animation

---

## Test User

Username:
test

Password:
test123

Capabilities:
- 2 service slots
- Educational/demo animation enabled
- Live communication visualization

---

# Slot Model

Total device slots:
5

Slot ownership:
- user001 → 3 slots
- test001 → 2 slots

Each slot stores:
- active flag
- owner user ID
- service ID
- password version counter
- server-generated creation nonce

---

# Password Generation

Passwords are deterministically regenerated using:

- SRAM startup entropy
- userId
- serviceId
- password version
- per-service creation nonce
- internal policy identifier

Current password policy:
- 16 characters
- upper/lowercase
- numbers
- symbols

Example:
qE45(9qe#3uY0ba2

---

# Security Model

## Secure Channel

Browser ↔ Server:
HTTPS/TLS

## Local Trusted Channel

Server ↔ Arduino:
UART serial communication

The UART connection is currently considered trusted/local.

---

# RSA Encryption

During login:
- browser generates RSA keypair
- browser sends public key to server
- browser keeps private key locally

When revealing passwords:
- Arduino generates plaintext password
- server encrypts with browser public key
- browser decrypts locally

The plaintext password is never persisted server-side.

---

# Live Communication Animation

The test user includes a visual educational mode showing:

1. HTTPS login requests
2. UART serial commands
3. PUF password generation
4. RSA encryption
5. HTTPS responses

The animation displays:
- encrypted packets
- trusted UART packets
- detailed explanations
- sender/receiver flow

---

# Health, Device Status, and Diagnostics

PUF Vault intentionally separates server health from Arduino health so the browser can distinguish a live Raspberry Pi server from a missing or unhealthy PUF device.

## Server Health

`GET /api/health` is a public Spring Boot liveness check. It returns quickly whenever the server process is alive and does not open, probe, or depend on the Arduino serial connection.

Example response:

```json
{
  "server": "OK",
  "service": "PUF Vault Server",
  "timestamp": "2026-05-29T12:00:00Z"
}
```

## Device Health

`GET /api/device/health` is an authenticated dashboard health check for the Arduino. It performs a lightweight `STATUS` command, catches serial failures, and still returns HTTP 200 while the Spring Boot server is alive. The dashboard uses this endpoint for the PUF device light.

Device states:

- `READY` → green device light, text `OK`
- `POWER_CYCLE_REQUIRED` → amber device light, text `Connected but needs Repower`
- `DISCONNECTED` → red device light, text `Arduino disconnected`
- any other state → amber device light with the returned device status

Example disconnected response:

```json
{
  "server": "OK",
  "deviceConnected": false,
  "deviceStatus": "DISCONNECTED",
  "deviceState": "DISCONNECTED",
  "lastError": "No Arduino serial port found at /dev/serial/by-id/...",
  "currentPort": "DISCONNECTED",
  "timestamp": "2026-05-29T12:00:00Z"
}
```

## Device Status

`GET /api/device/status` remains an authenticated manual/debug command that directly sends `STATUS` to the Arduino. It can fail visibly if the Arduino is unavailable and is not used for dashboard keep-alive polling.

## Diagnostics

`GET /api/device/diagnostics` is authenticated and reports serial diagnostics such as `currentPort`, `availablePorts`, `lastFailure`, `lastFailureAt`, `lastCommand`, and `lastResponse`. It is safe to view while the Arduino is disconnected so UART history and other dashboard panels remain usable.

---

# UART Monitoring

The dashboard includes a live UART transcript showing:

Sender → Receiver
Message

Example:

Server → Arduino
GENERATE_PASSWORD user001 github.com

Arduino → Server
OK PASSWORD xxxxx

---

# Running the System

## 1. Flash Arduino

See:
arduino/README.md

---

## 2. Start Spring Boot Server

See:
server/README.md

---

## 3. Start Client

See:
client/README.md

---

# Current Simplifications

This is still a prototype implementation.

Not yet implemented:
- fuzzy extractor ECC
- helper data
- secure enclave
- secure element
- mutual authentication
- anti-tamper protections
- hardware-backed key storage

---

# Future Ideas

Planned future evolutions:
- Arduino Nano ESP32
- BLE/NFC keychain mode
- mobile autofill integration
- challenge-response authentication
- stronger fuzzy extraction
- secure hardware enclave
- distributed synchronization