package com.pufvault.device;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fazecast.jSerialComm.SerialPort;
import com.pufvault.auth.UserConfig;

@Service
public class SerialDeviceService {

    private final UserConfig config;

    private SerialPort port;
    private BufferedReader reader;
    private OutputStream output;

    private String lastCommand = "";
    private String lastResponse = "";
    private String lastError = "";

    private Instant lastSuccessAt = null;
    private Instant lastFailureAt = null;
    private Instant lastOpenAt = null;

    private final LinkedList<Map<String, Object>> uartLog =
            new LinkedList<>();

    public SerialDeviceService(UserConfig config) {
        this.config = config;
    }

    private synchronized void ensureOpen() {

        if (isPortHealthy()) {
            return;
        }

        closeQuietly();
        openPort();
    }

    private boolean isPortHealthy() {
        return port != null
                && port.isOpen()
                && reader != null
                && output != null;
    }

    private void openPort() {

        String configuredPort =
                config.getSerial().getPort();

        port =
                SerialPort.getCommPort(
                        configuredPort
                );

        port.setBaudRate(
                config.getSerial().getBaud()
        );

        port.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_SEMI_BLOCKING,
                config.getSerial().getTimeoutMs(),
                config.getSerial().getTimeoutMs()
        );

        if (!port.openPort()) {

            lastError =
                    "Cannot open serial port "
                            + configuredPort;

            lastFailureAt = Instant.now();

            throw new RuntimeException(lastError);
        }

        reader =
                new BufferedReader(
                        new InputStreamReader(
                                port.getInputStream(),
                                StandardCharsets.UTF_8
                        )
                );

        output = port.getOutputStream();

        lastOpenAt = Instant.now();

        logUart(
                "Server",
                "Arduino",
                "SERIAL_PORT_OPEN"
        );

        try {

            Thread.sleep(1800);

            drain();

        } catch (Exception ignored) {}
    }

    public synchronized void reconnect() {

        logUart(
                "Server",
                "Arduino",
                "RECONNECT_REQUEST"
        );

        closeQuietly();

        openPort();
    }

    private void closeQuietly() {

        try {
            if (reader != null) {
                reader.close();
            }
        } catch (Exception ignored) {}

        try {
            if (output != null) {
                output.close();
            }
        } catch (Exception ignored) {}

        try {
            if (port != null && port.isOpen()) {
                port.closePort();
            }
        } catch (Exception ignored) {}

        logUart(
                "Server",
                "Arduino",
                "SERIAL_PORT_CLOSED"
        );

        reader = null;
        output = null;
        port = null;
    }

    private void drain() throws IOException {

        if (reader == null) {
            return;
        }

        long until =
                System.currentTimeMillis() + 300;

        while (
                System.currentTimeMillis() < until
                        && reader.ready()
        ) {

            String line = reader.readLine();

            if (line != null) {

                logUart(
                        "Arduino",
                        "Server",
                        line
                );
            }
        }
    }

    public synchronized List<String> commandMulti(
            String command,
            String endMarker
    ) {

        lastCommand = command;

        try {

            List<String> result =
                    commandMultiOnce(
                            command,
                            endMarker
                    );

            lastResponse =
                    String.join(" | ", result);

            lastSuccessAt = Instant.now();

            lastError = "";

            return result;

        } catch (RuntimeException firstFailure) {

            lastError =
                    firstFailure.getMessage();

            lastFailureAt = Instant.now();

            closeQuietly();

            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {}

            try {

                List<String> result =
                        commandMultiOnce(
                                command,
                                endMarker
                        );

                lastResponse =
                        String.join(" | ", result);

                lastSuccessAt = Instant.now();

                lastError = "";

                return result;

            } catch (RuntimeException secondFailure) {

                lastError =
                        "Serial command failed after reconnect attempt: "
                                + secondFailure.getMessage();

                lastFailureAt = Instant.now();

                throw new RuntimeException(
                        lastError,
                        secondFailure
                );
            }
        }
    }

    private List<String> commandMultiOnce(
            String command,
            String endMarker
    ) {

        ensureOpen();

        try {

            logUart(
                    "Server",
                    "Arduino",
                    command
            );

            output.write(
                    (command + "\n")
                            .getBytes(StandardCharsets.UTF_8)
            );

            output.flush();

            List<String> lines =
                    new ArrayList<>();

            long deadline =
                    System.currentTimeMillis()
                            + config.getSerial().getTimeoutMs();

            while (
                    System.currentTimeMillis()
                            < deadline
            ) {

                String line = reader.readLine();

                if (line == null) {
                    continue;
                }

                line = line.trim();

                if (line.isBlank()) {
                    continue;
                }

                logUart(
                        "Arduino",
                        "Server",
                        line
                );

                lines.add(line);

                if (endMarker == null) {

                    if (
                            line.startsWith("OK ")
                                    || line.startsWith("NOK ")
                                    || line.startsWith("PUFVAULT_")
                    ) {
                        break;
                    }

                } else {

                    if (
                            line.contains(endMarker)
                                    || line.startsWith("NOK ")
                    ) {
                        break;
                    }
                }
            }

            if (lines.isEmpty()) {

                throw new RuntimeException(
                        "No response from device"
                );
            }

            return lines;

        } catch (IOException e) {

            throw new RuntimeException(
                    "Serial I/O error: "
                            + e.getMessage(),
                    e
            );
        }
    }

    public String command(String command) {
        return commandMulti(command, null).get(0);
    }

    public synchronized Map<String, Object> diagnostics() {

        List<String> availablePorts =
                Arrays.stream(
                                SerialPort.getCommPorts()
                        )
                        .map(p ->
                                p.getSystemPortName()
                                        + " / "
                                        + p.getDescriptivePortName()
                        )
                        .toList();

        Map<String, Object> diagnostics =
                new LinkedHashMap<>();

        diagnostics.put(
                "configuredPort",
                config.getSerial().getPort()
        );

        diagnostics.put(
                "baud",
                config.getSerial().getBaud()
        );

        diagnostics.put(
                "timeoutMs",
                config.getSerial().getTimeoutMs()
        );

        diagnostics.put(
                "open",
                isPortHealthy()
        );

        diagnostics.put(
                "lastCommand",
                lastCommand
        );

        diagnostics.put(
                "lastResponse",
                lastResponse
        );

        diagnostics.put(
                "lastError",
                lastError
        );

        diagnostics.put(
                "lastOpenAt",
                lastOpenAt == null
                        ? ""
                        : lastOpenAt.toString()
        );

        diagnostics.put(
                "lastSuccessAt",
                lastSuccessAt == null
                        ? ""
                        : lastSuccessAt.toString()
        );

        diagnostics.put(
                "lastFailureAt",
                lastFailureAt == null
                        ? ""
                        : lastFailureAt.toString()
        );

        diagnostics.put(
                "availablePorts",
                availablePorts
        );

        return diagnostics;
    }

    public synchronized List<Map<String, Object>> uartLogs() {
        return new ArrayList<>(uartLog);
    }

    private synchronized void logUart(
            String sender,
            String receiver,
            String message
    ) {

        Map<String, Object> entry =
                new LinkedHashMap<>();

        entry.put(
                "timestamp",
                Instant.now().toString()
        );

        entry.put("sender", sender);

        entry.put("receiver", receiver);

        entry.put("message", message);

        uartLog.addFirst(entry);

        while (uartLog.size() > 200) {
            uartLog.removeLast();
        }
    }
}