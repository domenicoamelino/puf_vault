# PUF Vault Arduino/Raspberry Prototype

This package contains a complete first implementation of the simplified PUF Vault architecture:

```text
Browser client
  -> HTTPS/TLS
Raspberry Pi Java server
  -> UART serial
Arduino PUF device
```

## Architecture

### Client

Located in `client/`.

The browser client:

- logs in with an existing username/password
- generates an RSA-OAEP public/private key pair locally
- sends the public key to the server during login
- registers services
- requests generated passwords
- decrypts the encrypted password returned by the server
- copies the decrypted password to the clipboard

The private key stays in browser memory for this prototype.

### Java server

Located in `server/`.

The Spring Boot server:

- exposes REST APIs under `/api`
- authenticates existing users from `application.yml`
- issues a simple HMAC-signed bearer token
- communicates with the Arduino via UART
- claims a user slot on the Arduino
- registers service slots
- asks Arduino to generate/rotate passwords
- encrypts returned plaintext passwords using the user's browser-generated RSA public key

### Arduino device

Located in `arduino/PufVaultDevice/`.

The Arduino device:

- stores 2 users
- stores 5 service slots per user
- supports only `DEFAULT_16`
- captures SRAM startup data as PUF material
- stores per-user salt/helper data and service metadata in EEPROM
- derives deterministic 16-character passwords
- rotates passwords by repeatedly hashing the original digest via a version counter

## Default server login

Edit `server/src/main/resources/application.yml`.

Default demo user:

```text
username: demo
password: demo123
userId: user001
```

## UART protocol

Commands supported by Arduino:

```text
PING
STATUS
CAPABILITY
LIST_USERS
CREATE_USER <userId>
LIST_SERVICES <userId>
ADD_SERVICE <userId> <serviceId> DEFAULT_16
GENERATE_PASSWORD <userId> <serviceId>
ROTATE_SERVICE <userId> <serviceId>
WIPE_ALL
RESET_DEVICE
```

## Run server

```bash
cd server
mvn spring-boot:run
```

Adjust the serial port in `application.yml`, for example:

```yaml
pufvault:
  serial:
    port: "/dev/ttyACM0"
    baud: 115200
```

Common Raspberry Pi/Arduino ports:

```text
/dev/ttyACM0
/dev/ttyUSB0
```

## Run client

Open `client/index.html` with a local web server, for example:

```bash
cd client
python3 -m http.server 5500
```

Then open:

```text
http://localhost:5500
```

Use API endpoint:

```text
http://localhost:8080/api
```

## Important security notes

This is a working prototype, not a production-ready password vault.

Current simplifications:

- UART is trusted and plaintext between Raspberry Pi and Arduino.
- Password encryption happens on the Raspberry Pi, not on the Arduino.
- Helper data is a lightweight prototype structure, not a full ECC fuzzy extractor.
- Browser private key is kept in memory only and is lost on page refresh.
- Login uses demo HMAC tokens, not hardened production auth.
- Password is briefly visible on Raspberry Pi server memory.

Next hardening steps:

- replace the helper-data model with a real fuzzy extractor/ECC design
- use encrypted/authenticated UART packets
- store users in a real database
- hash server login passwords with Argon2/bcrypt
- persist browser private key securely or use WebAuthn/passkeys
- add device challenge/response
- add proper HTTPS certificate configuration on Raspberry Pi

