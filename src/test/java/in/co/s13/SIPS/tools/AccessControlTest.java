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

import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Blacklist evaluation and API permission bits.
 *
 * <p>Regression cover for the De Morgan slip in {@code APIHandler}:
 * {@code (!BLACKLIST.containsKey(ip) || !BLACKLIST.containsKey(uuid))}.
 * Because the UUID is asserted by the caller, a blacklisted host could send any
 * unknown UUID and make the right-hand side true, sailing past the check.
 * A blacklist must reject if <em>either</em> identifier is listed.
 */
class AccessControlTest {

    private static final Set<String> BLACKLIST = Set.of("10.0.0.9", "rogue-uuid");

    @Test
    void blocksListedIpEvenWhenUuidIsUnknown() {
        // The attack: blacklisted IP supplies a UUID that is not on the list.
        assertTrue(AccessControl.isBlacklisted(BLACKLIST, "10.0.0.9", "freshly-made-up-uuid"));
    }

    @Test
    void blocksListedUuidEvenFromAnUnlistedAddress() {
        assertTrue(AccessControl.isBlacklisted(BLACKLIST, "192.168.1.5", "rogue-uuid"));
    }

    @Test
    void blocksWhenBothAreListed() {
        assertTrue(AccessControl.isBlacklisted(BLACKLIST, "10.0.0.9", "rogue-uuid"));
    }

    @Test
    void allowsWhenNeitherIsListed() {
        assertFalse(AccessControl.isBlacklisted(BLACKLIST, "192.168.1.5", "good-uuid"));
    }

    @Test
    void toleratesNullIdentifiers() {
        assertFalse(AccessControl.isBlacklisted(BLACKLIST, null, null));
        assertTrue(AccessControl.isBlacklisted(BLACKLIST, null, "rogue-uuid"));
    }

    @Test
    void ignoresSurroundingWhitespace() {
        assertTrue(AccessControl.isBlacklisted(BLACKLIST, "  10.0.0.9 ", "x"));
    }

    @Test
    void emptyBlacklistBlocksNobody() {
        assertFalse(AccessControl.isBlacklisted(Set.of(), "10.0.0.9", "rogue-uuid"));
    }

    // ---- permission bits: 4 = read, 2 = write, 1 = execute ----

    @Test
    void readPermissionIsBitFour() {
        assertTrue(AccessControl.hasRead(4));
        assertTrue(AccessControl.hasRead(7));
        assertTrue(AccessControl.hasRead(6));
        assertFalse(AccessControl.hasRead(2));
        assertFalse(AccessControl.hasRead(3));
    }

    @Test
    void writePermissionIsBitTwo() {
        assertTrue(AccessControl.hasWrite(2));
        assertTrue(AccessControl.hasWrite(7));
        assertFalse(AccessControl.hasWrite(4));
        assertFalse(AccessControl.hasWrite(5));
    }

    @Test
    void executePermissionIsBitOne() {
        assertTrue(AccessControl.hasExecute(1));
        assertTrue(AccessControl.hasExecute(7));
        assertFalse(AccessControl.hasExecute(4));
        assertFalse(AccessControl.hasExecute(6));
    }

    @Test
    void zeroGrantsNothing() {
        assertFalse(AccessControl.hasRead(0));
        assertFalse(AccessControl.hasWrite(0));
        assertFalse(AccessControl.hasExecute(0));
    }

    @Test
    void negativePermissionsGrantNothing() {
        // A malformed key must not become a wildcard via sign bits.
        assertFalse(AccessControl.hasRead(-1));
        assertFalse(AccessControl.hasWrite(-1));
        assertFalse(AccessControl.hasExecute(-1));
    }
}
