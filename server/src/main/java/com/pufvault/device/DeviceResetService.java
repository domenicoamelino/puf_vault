package com.pufvault.device;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Service;

@Service
public class DeviceResetService {

    private static final long RESET_TIMEOUT_SECONDS = 15;
    private static final Path RESET_SCRIPT = Path.of("/usr/local/bin/pufvault-reset-device.py");

    public String reset() throws IOException {
        requireInstalledResetScript(RESET_SCRIPT);

        Process process = new ProcessBuilder(
                "/usr/bin/python3",
                "/usr/local/bin/pufvault-reset-device.py"
        )
                .redirectErrorStream(true)
                .start();

        CompletableFuture<String> output = CompletableFuture.supplyAsync(() -> {
            try {
                return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("Could not read reset script output", e);
            }
        });

        try {
            if (!process.waitFor(RESET_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException("Reset script timed out");
            }

            String scriptOutput = output.get(1, TimeUnit.SECONDS);

            if (process.exitValue() != 0) {
                throw new IOException("Reset script exited with code "
                        + process.exitValue()
                        + formatOutput(scriptOutput));
            }

            return scriptOutput;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException("Reset script execution was interrupted", e);
        } catch (ExecutionException | TimeoutException e) {
            process.destroyForcibly();
            throw new IOException("Could not capture reset script output", e);
        }
    }

    static void requireInstalledResetScript(Path script) throws IOException {
        if (!Files.isRegularFile(script) || !Files.isReadable(script)) {
            throw new IOException("Reset script is not installed or readable on the server host at "
                    + script
                    + ". Install it on the Raspberry Pi server before using Reset device.");
        }
    }

    private String formatOutput(String output) {
        if (output.isBlank()) {
            return "";
        }

        return ": " + output.strip();
    }
}
