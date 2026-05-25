# PUF Vault Arduino Device — Mac Flashing Guide

This guide explains how to flash and verify the Arduino-based PUF device from macOS.

## 1. Install Arduino IDE

Install Arduino IDE, or use Homebrew:

```bash
brew install --cask arduino-ide
```

## 2. Connect the Arduino

Connect the Arduino board via USB, then check the detected serial devices:

```bash
ls /dev/tty.*
```

Typical names are:

```text
/dev/tty.usbmodemXXXX
/dev/tty.usbserialXXXX
```

If you use a clone board with a CH340/CH341 USB chip, you may need the CH340 driver.

## 3. Open the Arduino sketch

Open the PUF Vault Arduino sketch in Arduino IDE.

The sketch should support commands such as:

```text
STATUS
CAPABILITY
CREATE_USER
DELETE_USER
LIST_USERS
ADD_SERVICE
DELETE_SERVICE
LIST_SERVICES
GENERATE_PASSWORD
ROTATE_SERVICE
RESET_DEVICE
```

## 4. Select board and port

In Arduino IDE:

```text
Tools → Board → select your Arduino board
Tools → Port  → select the detected /dev/tty.* port
```

For Arduino Uno, select `Arduino Uno`.

For Arduino Mega 2560 R3, select `Arduino Mega or Mega 2560`.

## 5. Upload the sketch

Click **Upload**.

After flashing, the board usually resets. This can temporarily disconnect and reconnect the serial port.

## 6. Verify serial output

Open Serial Monitor and set the baud rate to the value used in the sketch, for example:

```text
9600
115200
```

The Arduino sketch and Java server must use the same baud rate.

You should see something like:

```text
PUFVAULT_ARDUINO_READY
```

## 7. Close Serial Monitor before starting the server

Only one program can use the serial port at a time. Before running the Java server, close Arduino IDE Serial Monitor.

## 8. SRAM reset note

The current `RESET_DEVICE` command performs a software reset. For a true fresh SRAM startup state, a real power-cycle is better.

For the future Raspberry Pi setup, consider controlling Arduino power through a MOSFET, relay, or USB power switch.

## Troubleshooting

### Server cannot communicate after flashing

Run:

```bash
ls /dev/tty.*
```

The port may have changed after reflashing. Update the server `application.yml`.

### Server says it cannot open the port

Close Arduino Serial Monitor.

### Server was running while the board was unplugged

Use the diagnostics dashboard and click **Reconnect device**, or restart the server.

### Commands return `NOK UNKNOWN_COMMAND`

The Arduino sketch does not include that command, or the wrong sketch is flashed.
