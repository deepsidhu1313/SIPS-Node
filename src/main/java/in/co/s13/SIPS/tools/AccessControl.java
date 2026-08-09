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

import java.util.Collection;

/**
 * Access decisions for inbound requests, kept free of socket and global state
 * so they can be tested directly.
 *
 * <p>The blacklist check previously read
 * {@code (!BLACKLIST.containsKey(ip) || !BLACKLIST.containsKey(uuid))}. Since
 * the UUID is supplied by the caller rather than proven, a blacklisted host
 * only had to invent an unlisted UUID to make that disjunction true and pass
 * the check. A blacklist has to reject when <em>either</em> identifier matches.
 */
public final class AccessControl {

    public static final int READ = 4;
    public static final int WRITE = 2;
    public static final int EXECUTE = 1;

    private AccessControl() {
    }

    /**
     * @param blacklist listed IP addresses, hostnames and node UUIDs
     * @param ipAddress peer address observed on the socket
     * @param clientUUID UUID asserted by the caller, not verified
     * @return true if either identifier is listed
     */
    public static boolean isBlacklisted(Collection<String> blacklist, String ipAddress,
            String clientUUID) {
        if (blacklist == null || blacklist.isEmpty()) {
            return false;
        }
        return contains(blacklist, ipAddress) || contains(blacklist, clientUUID);
    }

    public static boolean hasRead(int permissions) {
        return holds(permissions, READ);
    }

    public static boolean hasWrite(int permissions) {
        return holds(permissions, WRITE);
    }

    public static boolean hasExecute(int permissions) {
        return holds(permissions, EXECUTE);
    }

    private static boolean contains(Collection<String> blacklist, String identifier) {
        return identifier != null && blacklist.contains(identifier.trim());
    }

    /**
     * A malformed or negative permission value grants nothing. Without the
     * positivity guard, -1 has every bit set and would read as full access.
     */
    private static boolean holds(int permissions, int bit) {
        return permissions > 0 && (permissions & bit) != 0;
    }
}
