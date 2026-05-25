# PUF Vault Server — Mac Run Guide

This guide explains how to run the Java Spring Boot server locally on macOS.

The server:

- exposes REST endpoints for the browser client
- handles login and token authentication
- communicates with the Arduino PUF device over UART/USB serial
- encrypts generated passwords with the browser-provided public key

## 1. Requirements

Check Java and Maven:

```bash
java -version
mvn -version
```

You need Java 17+.

Install with Homebrew if needed:

```bash
brew install openjdk@17 maven
```

## 2. Connect the Arduino

Connect the Arduino via USB and check the serial port:

```bash
ls /dev/tty.*
```

Look for something like:

```text
/dev/tty.usbmodem1101
/dev/tty.usbserial-XXXX
```

## 3. Configure the serial port

Open:

```bash
server/src/main/resources/application.yml
```

Set the serial port and baud rate:

```yaml
pufvault:
  serial:
    port: "/dev/tty.usbmodem1101"
    baud: 115200
    timeoutMs: 3000
```

Use the real port shown by:

```bash
ls /dev/tty.*
```

The baud rate must match the Arduino sketch.

## 4. Run the server

From the server folder:

```bash
cd pufvault-arduino/server
mvn spring-boot:run
```

The server should start on:

```text
http://localhost:8080
```

## 5. Test login with curl

```bash
curl -X POST http://localhost:8080/api/login \
-H "Content-Type: application/json" \
-d '{"username":"demo","password":"demo123"}'
```

Expected response:

```json
{
  "token": "...",
  "userId": "user001"
}
```

## 6. Test device status

Use the token from login:

```bash
curl http://localhost:8080/api/device/status \
-H "Authorization: Bearer YOUR_TOKEN_HERE"
```

Expected response:

```json
{
  "response": "OK DEVICE_READY"
}
```

## 7. Useful endpoints

```text
POST   /api/login
GET    /api/device/status
GET    /api/device/diagnostics
POST   /api/device/reconnect
POST   /api/device/claim-slot
DELETE /api/device/claim-slot
GET    /api/services
POST   /api/services
POST   /api/services/{serviceId}/generate
POST   /api/services/{serviceId}/rotate
DELETE /api/services/{serviceId}
```

## 8. Diagnostics endpoint

```text
GET /api/device/diagnostics
```

It returns:

```text
configuredPort
baud
timeoutMs
open
lastCommand
lastResponse
lastError
availablePorts
lastOpenAt
lastSuccessAt
lastFailureAt
```

Use this when the Arduino has been unplugged, reflashed, or reconnected.

## Troubleshooting

### `Required request header 'Authorization' is not present`

Login first and pass:

```text
Authorization: Bearer <token>
```

### `No static resource api/auth/login`

The correct endpoint is:

```text
/api/login
```

not:

```text
/api/auth/login
```

### Cannot open serial port

Check:

```bash
ls /dev/tty.*
```

Update `application.yml`, and close Arduino Serial Monitor.

### Arduino was unplugged and plugged back in

Use **Reconnect device** in the diagnostics dashboard, or restart the server.

### Java `Map.of` compile error

`Map.of(...)` supports up to 10 key/value pairs. Use `LinkedHashMap` for larger diagnostic maps.

### Browser cannot login but curl works

This is usually a client/cache/CORS issue. Hard refresh:

```text
Cmd + Shift + R
```

Ensure the client API endpoint is:

```text
http://localhost:8080/api
```
