# PUF Vault

PUF Vault is a lightweight password vault prototype based on a Physical Unclonable Function (PUF) running on Arduino hardware.

The system consists of:

- A web client (HTML/CSS/JS)
- A Java Spring Boot server running on Mac/Raspberry Pi
- An Arduino-based PUF device communicating over UART

The password is never stored permanently.
Passwords are deterministically regenerated from:

- SRAM startup entropy (PUF)
- Service identifier
- Rotation version
- Internal policy metadata

The server encrypts the generated password using the user's RSA public key before returning it to the browser.

---

# Architecture

Client Browser
↓ HTTPS/TLS
Spring Boot Server
↓ UART Serial
Arduino PUF Device

---

# Main Features

- Password generation without storing plaintext passwords
- RSA client-side encryption/decryption
- UART monitoring dashboard
- Device diagnostics
- Automatic server/device connection indicators
- Device repower detection
- Password rotation
- Service deletion
- Live UART transcript

---

# Current Device Model

The Arduino firmware currently supports:

- 1 fixed user
- 6 service slots
- Delete individual service slot
- Password rotation versioning
- UART ping/keep-alive

When a service is deleted:

- EEPROM metadata is updated
- Device enters POWER_CYCLE_REQUIRED state
- Dashboard shows:
  Connected but needs Repower

---

# Running the System

## 1. Flash Arduino

Follow:

arduino/README.md

## 2. Start Server

Follow:

server/README.md

## 3. Start Client

Follow:

client/README.md

---

# Security Notes

This is currently a prototype implementation.

Security assumptions:

- HTTPS/TLS between browser and server
- UART connection considered trusted
- Password encrypted with client RSA public key
- Password never persisted server-side

Current simplifications:

- No fuzzy extractor ECC
- No helper data
- No mutual authentication
- No secure element

---

# UART Monitoring

The dashboard exposes:

- Live UART messages
- Sender/receiver direction
- Device diagnostics
- Serial reconnection status
- Port information

Example:

Server → Arduino
ADD_SERVICE github.com

Arduino → Server
OK SERVICE_ADDED SLOT=0

---

# Device Status States

## Green

Device connected and operational.

## Amber

Device connected but requires repower.

## Red

Device disconnected or not responding.

---

# Future Work

Planned future evolutions:

- Arduino Nano ESP32
- Bluetooth/NFC password keychain
- Mobile autofill integration
- Secure challenge-response
- True fuzzy extractor
- Secure enclave integration
- Multi-device synchronization