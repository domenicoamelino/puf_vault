package com.pufvault.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeviceResetServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void missingScriptReturnsActionableServerHostMessage() {
        Path missingScript = tempDir.resolve("pufvault-reset-device.py");

        IOException error = assertThrows(
                IOException.class,
                () -> DeviceResetService.requireInstalledResetScript(missingScript)
        );

        assertEquals(
                "Reset script is not installed or readable on the server host at "
                        + missingScript
                        + ". Install it on the Raspberry Pi server before using Reset device.",
                error.getMessage()
        );
    }
}
