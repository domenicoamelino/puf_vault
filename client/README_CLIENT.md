# PUF Vault Client — Mac Run Guide

This guide explains how to run the HTML/CSS/JavaScript client locally on macOS.

The client:

- logs in to the Spring Boot server
- generates a browser-side RSA key pair
- sends the public key to the server
- receives encrypted passwords from the server
- decrypts passwords locally in the browser
- allows reveal/copy/update/delete service actions
- shows device diagnostics

## 1. Start the Java server first

```bash
cd pufvault-arduino/server
mvn spring-boot:run
```

The server should be available at:

```text
http://localhost:8080
```

## 2. Start the static client server

From the client folder:

```bash
cd pufvault-arduino/client
python3 -m http.server 5500
```

## 3. Open the client

Open:

```text
http://127.0.0.1:5500
```

Using `127.0.0.1` is recommended for local testing.

## 4. API endpoint

In the login form, use the base API endpoint:

```text
http://localhost:8080/api
```

Do not enter:

```text
http://localhost:8080/api/login
```

The JavaScript app automatically appends `/login`.

## 5. Default credentials

```text
username: demo
password: demo123
```

## 6. Main actions

### Claim device slot

Creates a user slot on the Arduino for the logged-in user.

### Remove device slot

Deletes the entire user slot from the Arduino, including service metadata. The server then sends `RESET_DEVICE`.

Note: this is currently a software reset. A true SRAM reset needs hardware power control.

### Add service

Registers a service name, for example:

```text
github.com
google.com
netflix.com
```

### Reveal

Asks the Arduino to generate the password through the server. The server encrypts it with the browser public key. The browser decrypts it and shows it next to the service.

### Copy

Copies the revealed password.

Some browsers, especially Safari in local development, may block clipboard access. If copy fails, the password remains visible for manual copy.

### Update password

Rotates the service version on the Arduino. The next generated password will be different.

### Delete service

Deletes only the selected service metadata from the Arduino.

## 7. Device diagnostics

The diagnostics panel can:

- refresh diagnostics
- check device status
- reconnect the serial device

Use this after:

- unplugging the Arduino
- reflashing the Arduino
- changing USB port
- seeing communication errors

## 8. Browser cache issues

If the UI does not update after editing `app.js` or `index.html`, hard refresh:

```text
Cmd + Shift + R
```

The HTML file uses a versioned script path:

```html
<script src="app.js?v=9"></script>
```

If needed, increment the version:

```html
<script src="app.js?v=10"></script>
```

## Troubleshooting

### Login fails in browser but works with curl

Check that the API endpoint is:

```text
http://localhost:8080/api
```

not:

```text
http://localhost:8080/api/login
```

Then hard refresh.

### CORS or fetch errors

Run the client from:

```text
http://127.0.0.1:5500
```

not directly from a file path.

### Reveal works but copy fails

This is likely browser clipboard permission. The password should still be visible next to the service.

### Services reappear after deletion

The Arduino did not actually delete the service metadata. Check server logs and diagnostics. The Arduino must support:

```text
DELETE_SERVICE userId serviceId
```

### Device disconnected

Use **Refresh diagnostics** and **Reconnect device**.

Also verify the serial port:

```bash
ls /dev/tty.*
```
