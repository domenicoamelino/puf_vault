# Arduino PUF Device

This firmware implements the hardware PUF engine for the PUF Vault project.

The board communicates with the Spring Boot server over UART serial communication.

---

# Hardware

Current target:
- Arduino Mega 2560 R3

Baud rate:
115200

---

# Main Features

- SRAM-based entropy extraction
- EEPROM metadata storage
- Deterministic password generation
- Password rotation
- Slot deletion
- User ownership management
- UART command protocol
- Device diagnostics
- Repower detection

---

# User Model

Two users are supported:

## Personal User
user001
3 slots

## Test User
test001
2 slots

Total slots:
5

---

# Slot Metadata

Each slot stores:
- active flag
- owner user ID
- service ID
- password version counter
- server-generated creation nonce (up to 31 UART-safe characters: `A-Z a-z 0-9 _ - .`)

---

# Password Generation

Passwords are regenerated from:
- SRAM startup entropy
- userId
- serviceId
- version counter
- per-service creation nonce
- policy identifier

Current password policy:
- 16 characters
- uppercase/lowercase
- numbers
- symbols

Example:
qE45(9qe#3uY0ba2

---

# Flashing the Firmware

## 1. Install Arduino IDE

https://www.arduino.cc/en/software

---

## 2. Install Required Libraries

Install:
- Crypto
- SHA256

---

# Uploading

1. Connect Arduino via USB
2. Select:
Tools → Board → Arduino Mega 2560
3. Select correct serial port
4. Upload sketch

---

# Verify Startup

Open Serial Monitor:
115200 baud
newline ending

Expected startup message:

PUFVAULT_ARDUINO_READY

---

# UART Commands

## Health

PING
STATUS
CAPABILITY

---

## Services

LIST_SERVICES user001

ADD_SERVICE user001 github.com 20260526T163012Z_9f2c1d3a

GENERATE_PASSWORD user001 github.com

ROTATE_SERVICE user001 github.com

DELETE_SERVICE user001 github.com

---

## Maintenance

WIPE_ALL

---

# Example Responses

OK PONG

OK READY

OK SERVICE_ADDED SLOT=0

OK PASSWORD xxxxx

OK SERVICE_ROTATED VERSION=1

OK SERVICE_DELETED POWER_CYCLE_REQUIRED

---

# Power Cycle Required

After deleting a service:
DELETE_SERVICE user001 github.com

the device enters:
NOK POWER_CYCLE_REQUIRED

until the board is power-cycled.

The web dashboard will display:
Connected but needs Repower

---

# Keep Alive

The server periodically checks:
STATUS

to validate that the Arduino is still operational.

---

# Internal Design Notes

Current simplifications:
- no fuzzy extractor ECC
- no helper data
- no anti-tamper protections
- no secure storage element

The SRAM startup state is directly used as entropy material.