# PUF Vault Web Client

Simple HTML/CSS/JavaScript frontend.

The browser:

- generates RSA keypair
- sends public key to server
- decrypts passwords locally
- displays diagnostics and UART logs

---

# Requirements

Python 3

or any static HTTP server.

---

# Run Client (Mac)

From client directory:

python3 -m http.server 5500

Open:

http://127.0.0.1:5500

---

# Login

Default demo credentials:

Username:

demo

Password:

demo123

---

# Main Features

- Add service
- Reveal password
- Rotate password
- Delete service
- UART monitor
- Device diagnostics
- Connection status indicators

---

# Password Handling

The server returns:

RSA encrypted password

The browser decrypts locally using:

window.crypto.subtle

The plaintext password is never stored.

---

# Dashboard Tabs

## Services

Manage vault entries.

## Diagnostics

Shows:

- serial port
- last command
- last response
- errors
- available ports

## UART Monitor

Live UART transcript.

## Logs

Frontend event logs.

---

# Connection Indicators

## Server

Green:

Backend reachable

Red:

Server disconnected

---

## PUF Device

Green:

Arduino operational

Amber:

Connected but needs Repower

Red:

Arduino disconnected

---

# Browser Notes

After frontend updates:

Mac hard refresh:

Cmd + Shift + R

to avoid stale cached JS.