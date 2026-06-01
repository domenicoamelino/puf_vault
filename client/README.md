# PUF Vault Web Client

The PUF Vault client is a lightweight HTML/CSS/JavaScript frontend.

The browser is responsible for:
- generating RSA keys
- communicating with the server
- decrypting passwords locally
- visualizing UART communication
- displaying diagnostics and device health

---

# Requirements

- Python 3
OR
- any static HTTP server

---

# Run Client (Mac)

From the client directory:

python3 -m http.server 5500

Then open:

http://127.0.0.1:5500

---

# Default Login Accounts

## Personal User

Username:
demo

Password:
demo123

Capabilities:
- 3 slots
- normal UI
- no animation

---

## Test User

Username:
test

Password:
test123

Capabilities:
- 2 slots
- educational animation mode enabled
- live communication visualization

---

# Main Features

- Login
- Add service
- Reveal password
- Rotate password
- Delete service
- UART monitor
- Device diagnostics
- Authenticated device reset/power-cycle control
- Connection indicators
- Live communication animation

---

# Connection Indicators

The dashboard uses two separate health checks so the web client can show a live server even when the Arduino is unplugged.

## Server Connection

The server indicator calls `GET /api/health`. This endpoint only checks Spring Boot liveness and never depends on Arduino connectivity.

Green:
server reachable

Red:
server disconnected

---

## PUF Device Connection

The PUF device indicator calls authenticated `GET /api/device/health` only after the server is reachable and the user has a token. The dashboard no longer uses `/api/device/status` for keep-alive polling.

Green:
Arduino operational (`deviceState` is `READY`)

Amber:
connected but requires repower (`deviceState` is `POWER_CYCLE_REQUIRED`)

Red:
Arduino disconnected (`deviceState` is `DISCONNECTED`)

`GET /api/device/status` remains available as a manual/debug command, and diagnostics continue to display `currentPort`, `availablePorts`, `lastFailure`, `lastFailureAt`, `lastCommand`, and `lastResponse` if the Arduino is disconnected.

## Reset Device Control

The post-login Vault controls provide a visible yellow **Reset device** button near **Refresh services**. After confirmation, the browser sends authenticated `POST /api/reset_device`, disables the button while the operation is running, logs the result, and refreshes device health and diagnostics after a short delay.

The Raspberry Pi server expects the GPIO script at `/usr/local/bin/pufvault-reset-device.py`. Install and prepare it on the Pi with:

```bash
sudo apt install python3-gpiozero
sudo chmod +x /usr/local/bin/pufvault-reset-device.py
```

Before using the web control, verify the script manually:

```bash
/usr/bin/python3 /usr/local/bin/pufvault-reset-device.py
```

Expected output:

```text
RESET_START
RESET_DONE
```

> **Hardware warning:** The GPIO must drive a relay, MOSFET, or transistor rather than Arduino power directly. A true SRAM PUF cold-start requires physically cutting board power; toggling only the Arduino `RESET` pin is not sufficient.

---

# Browser-side RSA Encryption

During login:
- browser generates RSA keypair
- browser sends public key to server
- browser keeps private key locally

During password reveal:
- server returns RSA encrypted password
- browser decrypts locally using WebCrypto

The plaintext password is never stored.

---

# Live Communication Animation

Visible only for the test user.

The animation visualizes:
- HTTPS/TLS encrypted communication
- UART communication
- password generation
- RSA encryption
- response delivery

---

# Packet Types

## Encrypted Packet

Blue packet with lock icon.

Represents:
- HTTPS/TLS communication
- RSA encrypted password

---

## UART Packet

Grey packet.

Represents:
- trusted local serial communication

---

# Animation Steps

Example password reveal flow:

1. Browser sends HTTPS request
2. Server sends UART command
3. Arduino regenerates password
4. Server encrypts password with RSA
5. Browser decrypts locally

Each step includes:
- sender
- receiver
- security explanation
- encryption state

---

# Dashboard Tabs

## Services

Manage password slots.

---

## Diagnostics

Shows:
- serial port
- last command
- last response
- errors
- available ports

---

## UART Monitor

Displays:
- sender
- receiver
- UART message
- timestamp

Example:

Server → Arduino
GENERATE_PASSWORD user001 github.com

Arduino → Server
OK PASSWORD xxxxx

---

## Logs

Frontend event logging.

---

# Browser Notes

After frontend updates:
hard refresh browser cache:

Mac:
Cmd + Shift + R

to avoid stale JavaScript cache issues.

---

# Current Simplifications

Current prototype limitations:
- no persistent sessions
- no secure enclave
- no fuzzy extractor ECC
- no helper data
- UART considered trusted/local

---

# Future Ideas

Planned future improvements:
- mobile support
- BLE/NFC mode
- keychain hardware mode
- password autofill
- animated topology improvements
- WebSocket live UART streaming
- secure hardware-backed storage