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
package in.co.s13.SIPS.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Resolution of the {@code target} attribute written into each generated Ant
 * build file.
 *
 * <p>Regression cover for {@code ParallelProcess.getVersion()}, which did
 * {@code version.substring(0, version.indexOf('.'))}. On a JDK that reports a
 * dotless version such as "17" that is {@code substring(0, -1)} and throws
 * StringIndexOutOfBoundsException, so no task could ever be compiled.
 */
class JavaTargetTest {

    @ParameterizedTest
    @CsvSource({
        "1.8.0_201, 1.8",
        "1.8.0,     1.8",
        "9,         9",
        "9.0.4,     9",
        "11.0.2,    11",
        "17,        17",
        "21,        21",
        "21.0.12,   21",
        "24-ea,     24",
        "25-internal, 25"
    })
    void resolvesFeatureVersion(String reported, String expected) {
        assertEquals(expected, JavaTarget.forVersion(reported));
    }

    @Test
    void dotlessVersionDoesNotThrow() {
        // The exact input that broke the original implementation.
        assertEquals("17", JavaTarget.forVersion("17"));
    }

    @Test
    void unparseableVersionFallsBackToTheRunningRuntime() {
        String fallback = JavaTarget.current();
        assertEquals(fallback, JavaTarget.forVersion("not-a-version"));
        assertEquals(fallback, JavaTarget.forVersion(null));
        assertEquals(fallback, JavaTarget.forVersion(""));
    }

    @Test
    void currentRuntimeIsReportedAsAValidJavacTarget() {
        String current = JavaTarget.current();
        // Must be either "1.8" or a bare feature number; anything else is not a
        // legal javac target and would fail the generated Ant build.
        assertEquals(true, current.equals("1.8") || current.matches("\\d+"),
                "not a legal javac target: " + current);
    }
}
