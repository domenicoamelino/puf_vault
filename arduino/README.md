# Arduino PUF Device

This firmware implements the hardware PUF password engine.

The board communicates with the Spring Boot server over UART.

---

# Hardware

Current target:

- Arduino Mega 2560 R3

Baud rate:

115200

---

# Features

- SRAM-based PUF entropy
- EEPROM slot metadata storage
- Deterministic password generation
- Password rotation support
- Slot deletion
- Keep-alive ping
- Device diagnostics

---

# Slot Model

Current firmware:

- 1 fixed user
- 6 service slots

Each slot stores:

- active flag
- serviceId
- password rotation version

---

# Password Generation

Passwords are derived from:

- SRAM startup state
- serviceId
- internal version counter
- policy id

The generated password format:

16 characters
A-Z a-z 0-9 symbols

Example:

qE45(9qe#3uY0ba2

---

# Flashing the Firmware

## 1. Install Arduino IDE

https://www.arduino.cc/en/software

## 2. Install Libraries

Install:

- Crypto
- SHA256

---

# Upload

1. Connect Arduino via USB
2. Select board:

Tools → Board → Arduino Mega 2560

3. Select serial port
4. Upload sketch

---

# Verify UART

Open:

Tools → Serial Monitor

Settings:

- 115200 baud
- Newline ending

You should see:

PUFVAULT_ARDUINO_READY

---

# Manual UART Commands

PING
STATUS
CAPABILITY
LIST_SERVICES
ADD_SERVICE github.com
GENERATE_PASSWORD github.com
ROTATE_SERVICE github.com
DELETE_SERVICE github.com
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

DELETE_SERVICE github.com

The device enters:

NOK POWER_CYCLE_REQUIRED

until power-cycled.

The web dashboard will show:

Connected but needs Repower