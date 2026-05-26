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
- Connection indicators
- Live communication animation

---

# Connection Indicators

## Server Connection

Green:
server reachable

Red:
server disconnected

---

## PUF Device Connection

Green:
Arduino operational

Amber:
connected but requires repower

Red:
Arduino disconnected

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