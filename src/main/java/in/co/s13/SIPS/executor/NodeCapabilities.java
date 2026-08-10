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

import in.co.s13.sips.lib.common.datastructure.Node;
import in.co.s13.sips.lib.protocol.Protocol;
import in.co.s13.sips.lib.protocol.Protocol.Feature;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which of the live nodes can actually run a given kind of work.
 *
 * <p>Applied when work is <em>scheduled</em>, not when it is sent. Sending a
 * chunk and having it fail on arrival costs the whole round trip, and the
 * failure appears in a log on the machine that could not run it — the one place
 * nobody looks. Filtering first means the scheduler simply divides the work
 * among the nodes that can do it.
 *
 * <p>A node that has never announced a protocol version is not assumed to be
 * old on principle: it is asked what the feature costs to get wrong. Something
 * an older node would ignore is worth sending anyway; something it would accept
 * and then fail is not. That distinction lives in {@link Protocol.Compatibility}.
 */
public final class NodeCapabilities {

    private NodeCapabilities() {
    }

    /** The nodes worth giving this kind of work to. */
    public static List<Node> capableOf(Feature feature, Collection<Node> nodes) {
        List<Node> capable = new ArrayList<>();
        if (nodes == null) {
            return capable;
        }
        for (Node node : nodes) {
            if (node != null && Protocol.canSend(feature, node.getProtocolVersion())) {
                capable.add(node);
            }
        }
        return capable;
    }

    /** The same, keyed by uuid, in the shape a scheduler is handed. */
    public static ConcurrentHashMap<String, Node> capableOf(Feature feature,
            ConcurrentHashMap<String, Node> nodes) {
        ConcurrentHashMap<String, Node> capable = new ConcurrentHashMap<>();
        if (nodes == null) {
            return capable;
        }
        nodes.forEach((uuid, node) -> {
            if (node != null && Protocol.canSend(feature, node.getProtocolVersion())) {
                capable.put(uuid, node);
            }
        });
        return capable;
    }

    /**
     * Why each excluded node was excluded, by hostname.
     *
     * <p>Worth reporting rather than silently scheduling around: a cluster
     * quietly running at half its size because half of it was never upgraded is
     * a performance mystery, not an obvious fault.
     */
    public static Map<String, String> refusals(Feature feature, Collection<Node> nodes) {
        Map<String, String> refused = new LinkedHashMap<>();
        if (nodes == null) {
            return refused;
        }
        for (Node node : nodes) {
            if (node != null && !Protocol.canSend(feature, node.getProtocolVersion())) {
                refused.put(nameOf(node), Protocol.refusalReason(feature,
                        node.getProtocolVersion()));
            }
        }
        return refused;
    }

    /** A one-line summary for a job log, or empty when every node can help. */
    public static String summarise(Feature feature, Collection<Node> nodes) {
        Map<String, String> refused = refusals(feature, nodes);
        if (refused.isEmpty()) {
            return "";
        }
        int total = nodes == null ? 0 : nodes.size();
        return refused.size() + " of " + total + " nodes cannot run "
                + feature.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ')
                + " and were left out: " + String.join(", ", refused.keySet())
                + ". " + Protocol.refusalReason(feature, Protocol.UNKNOWN);
    }

    private static String nameOf(Node node) {
        try {
            String hostname = node.getHostname();
            return hostname == null || hostname.isBlank() ? node.getUuid() : hostname;
        } catch (RuntimeException ex) {
            return String.valueOf(node.getUuid());
        }
    }
}
