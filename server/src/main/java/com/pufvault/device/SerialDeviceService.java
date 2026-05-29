package com.pufvault.device;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
    private String currentPortIdentifier = "";

    private String lastCommand = "";
    private String lastResponse = "";
    private String lastError = "";
    private String lastFailure = "";

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

        List<PortCandidate> candidates =
                serialPortCandidates();

        List<String> attemptedPorts =
                new ArrayList<>();

        for (PortCandidate candidate : candidates) {

            attemptedPorts.add(candidate.description());

            try {

                openCandidate(candidate);

                if (isPufVaultDevice()) {

                    currentPortIdentifier =
                            candidate.identifier();

                    lastOpenAt = Instant.now();

                    logUart(
                            "Server",
                            "Arduino",
                            "SERIAL_PORT_OPEN "
                                    + candidate.description()
                    );

                    return;
                }

                logUart(
                        "Server",
                        "Arduino",
                        "SERIAL_PORT_SKIPPED "
                                + candidate.description()
                );

            } catch (Exception ignored) {

                // Try the next available serial port.

            } finally {

                if (!isPortHealthy()
                        || currentPortIdentifier.isBlank()) {
                    closeQuietly();
                }
            }
        }

        String message =
                attemptedPorts.isEmpty()
                        ? "No serial ports were found"
                        : "No PUF Vault Arduino detected. Tried: "
                                + String.join(", ", attemptedPorts);

        lastError = message;
        lastFailure = message;
        lastFailureAt = Instant.now();

        throw new RuntimeException(message);
    }

    private void openCandidate(PortCandidate candidate)
            throws IOException, InterruptedException {

        port =
                SerialPort.getCommPort(
                        candidate.identifier()
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
            throw new IOException(
                    "Unable to open "
                            + candidate.description()
            );
        }

        reader =
                new BufferedReader(
                        new InputStreamReader(
                                port.getInputStream(),
                                StandardCharsets.UTF_8
                        )
                );

        output = port.getOutputStream();

        Thread.sleep(1800);

        drain();
    }

    private boolean isPufVaultDevice()
            throws IOException {

        if (!isPortHealthy()) {
            return false;
        }

        logUart(
                "Server",
                "Arduino",
                "STATUS"
        );

        output.write(
                "STATUS\n".getBytes(StandardCharsets.UTF_8)
        );

        output.flush();

        long deadline =
                System.currentTimeMillis()
                        + config.getSerial().getTimeoutMs();

        while (System.currentTimeMillis() < deadline) {

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

            if (line.startsWith("OK READY")
                    || line.startsWith("NOK POWER_CYCLE_REQUIRED")) {
                lastResponse = line;
                lastSuccessAt = Instant.now();
                lastError = "";
                return true;
            }
        }

        return false;
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
        currentPortIdentifier = "";
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
            lastFailure = lastError;

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
                lastFailure = lastError;

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

    public synchronized Map<String, Object> safeHealthCheck() {

        Map<String, Object> health =
                baseDeviceHealth();

        try {

            String status = command("STATUS");

            health.put("deviceConnected", true);
            health.put("deviceStatus", status);
            health.put("deviceState", deviceState(status));
            health.put("lastError", "");
            health.put("lastFailure", lastFailure);
            health.put("currentPort", currentPort());

        } catch (Exception e) {

            String message = e.getMessage() == null
                    ? e.getClass().getSimpleName()
                    : e.getMessage();

            lastError = message;
            lastFailure = message;
            lastFailureAt = Instant.now();

            health.put("deviceConnected", false);
            health.put("deviceStatus", "DISCONNECTED");
            health.put("deviceState", "DISCONNECTED");
            health.put("lastError", message);
            health.put("lastFailure", message);
            health.put("currentPort", "DISCONNECTED");
        }

        health.put("lastFailureAt", formatInstant(lastFailureAt));
        health.put("lastCommand", lastCommand);
        health.put("lastResponse", lastResponse);
        health.put("timestamp", Instant.now().toString());

        return health;
    }

    private Map<String, Object> baseDeviceHealth() {

        Map<String, Object> health =
                new LinkedHashMap<>();

        health.put("server", "OK");
        health.put("deviceConnected", false);
        health.put("deviceStatus", "DISCONNECTED");
        health.put("deviceState", "DISCONNECTED");
        health.put("lastError", lastError);
        health.put("lastFailure", lastFailure);
        health.put("currentPort", currentPort());
        health.put("configuredPort", config.getSerial().getPort());
        health.put("lastFailureAt", formatInstant(lastFailureAt));
        health.put("lastCommand", lastCommand);
        health.put("lastResponse", lastResponse);
        health.put("timestamp", Instant.now().toString());

        return health;
    }

    private String deviceState(String status) {

        if ("OK READY".equals(status)) {
            return "READY";
        }

        if ("NOK POWER_CYCLE_REQUIRED".equals(status)) {
            return "POWER_CYCLE_REQUIRED";
        }

        return "UNKNOWN";
    }

    private String currentPort() {

        if (!isPortHealthy()) {
            return "DISCONNECTED";
        }

        if (currentPortIdentifier != null
                && !currentPortIdentifier.isBlank()) {
            return currentPortIdentifier;
        }

        return port.getSystemPortName();
    }

    private String formatInstant(Instant value) {
        return value == null ? "" : value.toString();
    }

    private List<PortCandidate> serialPortCandidates() {

        List<PortCandidate> candidates =
                new ArrayList<>();

        String configuredPort =
                config.getSerial().getPort();

        if (isManualPort(configuredPort)) {
            candidates.add(
                    new PortCandidate(
                            configuredPort,
                            configuredPort,
                            1000
                    )
            );
        }

        try {

            Arrays.stream(SerialPort.getCommPorts())
                    .map(this::portCandidate)
                    .sorted(
                            Comparator.comparingInt(
                                    PortCandidate::score
                            ).reversed()
                    )
                    .forEach(candidates::add);

        } catch (Exception e) {

            String message = e.getMessage() == null
                    ? e.getClass().getSimpleName()
                    : e.getMessage();

            lastError = message;
            lastFailure = message;
            lastFailureAt = Instant.now();
        }

        return candidates.stream()
                .filter(candidate ->
                        candidate.identifier() != null
                                && !candidate.identifier().isBlank()
                )
                .collect(
                        java.util.stream.Collectors.collectingAndThen(
                                java.util.stream.Collectors.toMap(
                                        PortCandidate::identifier,
                                        candidate -> candidate,
                                        (first, ignored) -> first,
                                        LinkedHashMap::new
                                ),
                                map -> new ArrayList<>(map.values())
                        )
                );
    }

    private boolean isManualPort(String configuredPort) {
        return configuredPort != null
                && !configuredPort.isBlank()
                && !"auto".equalsIgnoreCase(configuredPort);
    }

    private PortCandidate portCandidate(SerialPort serialPort) {

        String identifier = serialPort.getSystemPortName();

        String description =
                identifier
                        + " / "
                        + serialPort.getDescriptivePortName();

        return new PortCandidate(
                identifier,
                description,
                portScore(identifier, description)
        );
    }

    private int portScore(
            String identifier,
            String description
    ) {

        String value =
                (identifier + " " + description)
                        .toLowerCase();

        int score = 0;

        if (value.contains("arduino")) {
            score += 100;
        }

        if (value.contains("ttyacm")) {
            score += 80;
        }

        if (value.contains("ttyusb")) {
            score += 70;
        }

        if (value.contains("usbmodem")) {
            score += 70;
        }

        if (value.contains("wch")
                || value.contains("ch340")
                || value.contains("cp210")
                || value.contains("usb serial")) {
            score += 50;
        }

        return score;
    }

    private List<String> availablePorts() {

        try {
            return Arrays.stream(
                            SerialPort.getCommPorts()
                    )
                    .map(p ->
                            p.getSystemPortName()
                                    + " / "
                                    + p.getDescriptivePortName()
                    )
                    .toList();
        } catch (Exception e) {

            String message = e.getMessage() == null
                    ? e.getClass().getSimpleName()
                    : e.getMessage();

            lastError = message;
            lastFailure = message;
            lastFailureAt = Instant.now();

            return List.of(
                    "Unable to list serial ports: " + message
            );
        }
    }

    public synchronized Map<String, Object> diagnostics() {

        List<String> availablePorts = availablePorts();

        Map<String, Object> diagnostics =
                new LinkedHashMap<>();

        diagnostics.put(
                "configuredPort",
                config.getSerial().getPort()
        );

        diagnostics.put(
                "portDetection",
                isManualPort(config.getSerial().getPort())
                        ? "manual-with-auto-fallback"
                        : "auto"
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
                "currentPort",
                currentPort()
        );

        diagnostics.put(
                "lastFailure",
                lastFailure
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
                formatInstant(lastOpenAt)
        );

        diagnostics.put(
                "lastSuccessAt",
                formatInstant(lastSuccessAt)
        );

        diagnostics.put(
                "lastFailureAt",
                formatInstant(lastFailureAt)
        );

        diagnostics.put(
                "availablePorts",
                availablePorts
        );

        return diagnostics;
    }

    private record PortCandidate(
            String identifier,
            String description,
            int score
    ) {}

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