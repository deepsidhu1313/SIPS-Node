/*
 * Copyright (C) 2026 Navdeep Singh Sidhu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package in.co.s13.SIPS.executor;

import in.co.s13.SIPS.tools.Platform;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Generation of the per-platform task executor scripts.
 *
 * <p>{@code TaskServer} previously wrote the POSIX script only when
 * {@code OS_Name == 2} (Linux) and fell through to writing a Windows
 * {@code .bat} for everything else, so a macOS node was handed a batch file it
 * could not run.
 */
class ExecutorScriptsTest {

    @ParameterizedTest
    @ValueSource(strings = {"MACOS", "LINUX", "SOLARIS"})
    void posixPlatformsGetAShellScript(String name) {
        Platform platform = Platform.valueOf(name);
        assertEquals("process-executor.sh", ExecutorScripts.executorFileName(platform));

        String script = ExecutorScripts.executorScript(platform);
        assertTrue(script.startsWith("#!/bin/sh"), "must be a POSIX sh script");
        assertFalse(script.contains("#!/bin/bash"), "bash is absent on stock Solaris");
    }

    @Test
    void windowsGetsABatchScript() {
        assertEquals("process-executor.bat", ExecutorScripts.executorFileName(Platform.WINDOWS));

        String script = ExecutorScripts.executorScript(Platform.WINDOWS);
        assertTrue(script.contains("@echo off"));
        assertFalse(script.contains("#!/bin/sh"));
    }

    @Test
    void posixScriptQuotesItsArgumentSoPathsWithSpacesSurvive() {
        String script = ExecutorScripts.executorScript(Platform.LINUX);
        assertTrue(script.contains("\"$1\""), "unquoted $1 breaks on paths with spaces");
    }

    @Test
    void everyScriptInvokesAnt() {
        for (Platform platform : new Platform[]{Platform.LINUX, Platform.MACOS,
            Platform.SOLARIS, Platform.WINDOWS}) {
            assertTrue(ExecutorScripts.executorScript(platform).contains("ant"),
                    platform + " script must run ant");
        }
    }

    /**
     * The old simulate script ran "bash ant", which looks for a file named
     * "ant" in the current directory rather than the ant on PATH.
     */
    @Test
    void simulateScriptInvokesAntDirectlyNotThroughBash() {
        String script = ExecutorScripts.simulateScript(Platform.LINUX);
        assertFalse(script.contains("bash ant"), "must not shell out to a local file named ant");
        assertTrue(script.contains("ant "), "should call ant with its argument");
    }

    @Test
    void unsupportedPlatformIsRejected() {
        assertThrows(IllegalStateException.class,
                () -> ExecutorScripts.executorScript(Platform.UNKNOWN));
    }

    @Test
    void writesScriptsAndMarksPosixOnesExecutable(@TempDir Path binDir) throws IOException {
        ExecutorScripts.install(Platform.LINUX, binDir);

        Path executor = binDir.resolve("process-executor.sh");
        assertTrue(Files.exists(executor), "executor script should be written");
        assertTrue(Files.exists(binDir.resolve("simulate.sh")), "simulate script should be written");
        assertTrue(Files.isExecutable(executor), "script must be executable to run");
    }

    @Test
    void installIsIdempotentAndOverwritesStaleContent() throws IOException, InterruptedException {
        Path binDir = Files.createTempDirectory("sips-bin");
        Path executor = binDir.resolve("process-executor.sh");
        Files.writeString(executor, "stale content from an older version");

        ExecutorScripts.install(Platform.LINUX, binDir);

        assertEquals(ExecutorScripts.executorScript(Platform.LINUX), Files.readString(executor));
    }

    @Test
    void installCreatesTheBinDirectoryIfMissing(@TempDir Path parent) throws IOException {
        Path binDir = parent.resolve("nested").resolve("bin");
        ExecutorScripts.install(Platform.LINUX, binDir);
        assertTrue(Files.exists(binDir.resolve("process-executor.sh")));
    }
}
