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

import in.co.s13.sips.lib.common.datastructure.LiveNode;
import in.co.s13.sips.lib.common.datastructure.Node;
import in.co.s13.sips.lib.protocol.Protocol;
import in.co.s13.sips.lib.protocol.Protocol.Feature;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Scheduling around nodes that cannot run the work.
 *
 * <p>A half-upgraded cluster is the normal state during a rollout, and it is the
 * state nobody tests. Getting this wrong is expensive in a particular way: the
 * chunk is sent, accepted, and fails on arrival, and the reason lands in a log
 * on the machine that could not run it.
 */
class NodeCapabilitiesTest {

    private static Node node(String uuid, int protocolVersion) {
        LiveNode live = new LiveNode(uuid, "host-" + uuid, "linux", "cpu", 4, 0,
                8L << 30, 4L << 30, 512L << 30, 256L << 30, new JSONObject(),
                System.currentTimeMillis(), 0.1);
        live.setProtocolVersion(protocolVersion);
        return live;
    }

    /** A node exactly as it arrives from a peer that never announced a version. */
    private static Node nodeFromAnOlderRelease(String uuid) {
        return new LiveNode(uuid, "host-" + uuid, "linux", "cpu", 4, 0,
                8L << 30, 4L << 30, 512L << 30, 256L << 30, new JSONObject(),
                System.currentTimeMillis(), 0.1);
    }

    @Test
    void anodeThatNeverAnnouncedIsUnknownNotCurrent() {
        // The default has to be pessimistic. Assuming current is the bug.
        assertEquals(Protocol.UNKNOWN, nodeFromAnOlderRelease("old").getProtocolVersion());
    }

    @Test
    void onlyCapableNodesAreOfferedWorkThatWouldFailElsewhere() {
        List<Node> cluster = List.of(
                node("new-1", Protocol.VERSION),
                nodeFromAnOlderRelease("old-1"),
                node("new-2", Protocol.VERSION));

        List<Node> capable = NodeCapabilities.capableOf(Feature.WASM_TASKS, cluster);

        assertEquals(2, capable.size());
        assertTrue(capable.stream().allMatch(n -> n.getUuid().startsWith("new")));
    }

    @Test
    void everyNodeIsOfferedWorkAnOlderOneWouldSafelyIgnore() {
        // Early exit is the case: an older node drops the message and its chunk
        // runs to completion. Excluding it would shrink the cluster for nothing.
        List<Node> cluster = List.of(node("new", Protocol.VERSION),
                nodeFromAnOlderRelease("old"));

        assertEquals(2, NodeCapabilities.capableOf(Feature.EARLY_EXIT, cluster).size());
        assertTrue(NodeCapabilities.refusals(Feature.EARLY_EXIT, cluster).isEmpty());
    }

    @Test
    void theSchedulersViewIsFilteredTheSameWay() {
        ConcurrentHashMap<String, Node> cluster = new ConcurrentHashMap<>();
        cluster.put("new", node("new", Protocol.VERSION));
        cluster.put("old", nodeFromAnOlderRelease("old"));

        ConcurrentHashMap<String, Node> capable =
                NodeCapabilities.capableOf(Feature.STAGED_JOBS, cluster);

        assertEquals(1, capable.size());
        assertTrue(capable.containsKey("new"));
    }

    @Test
    void anExcludedNodeIsNamedAlongWithWhatToDoAboutIt() {
        // A cluster quietly running at half its size is a performance mystery,
        // not an obvious fault. It has to say something.
        List<Node> cluster = List.of(node("new", Protocol.VERSION),
                nodeFromAnOlderRelease("old"));

        Map<String, String> refused = NodeCapabilities.refusals(Feature.WASM_TASKS, cluster);

        assertEquals(1, refused.size());
        assertTrue(refused.containsKey("host-old"));
        assertTrue(refused.get("host-old").toLowerCase().contains("upgrade"),
                refused.get("host-old"));
    }

    @Test
    void theSummaryCountsWhatWasLeftOut() {
        List<Node> cluster = List.of(node("a", Protocol.VERSION),
                nodeFromAnOlderRelease("b"), nodeFromAnOlderRelease("c"));

        String summary = NodeCapabilities.summarise(Feature.WASM_TASKS, cluster);

        assertTrue(summary.startsWith("2 of 3 nodes"), summary);
        assertTrue(summary.contains("host-b") && summary.contains("host-c"), summary);
    }

    @Test
    void aFullyUpgradedClusterSaysNothing() {
        // Silence when there is nothing to report; a warning printed every cycle
        // is a warning nobody reads.
        List<Node> cluster = List.of(node("a", Protocol.VERSION), node("b", Protocol.VERSION));

        assertEquals("", NodeCapabilities.summarise(Feature.WASM_TASKS, cluster));
    }

    @Test
    void anEmptyOrAbsentClusterIsNotAnError() {
        assertTrue(NodeCapabilities.capableOf(Feature.WASM_TASKS, List.of()).isEmpty());
        assertTrue(NodeCapabilities.capableOf(Feature.WASM_TASKS, (List<Node>) null).isEmpty());
        assertTrue(NodeCapabilities.refusals(Feature.WASM_TASKS, null).isEmpty());
    }

    @Test
    void aNodeAnnouncesItselfThroughThePingReply() {
        // The round trip that makes any of this work, in the shape the wire uses.
        JSONObject pingReply = Protocol.stamp(new JSONObject().put("HOSTNAME", "n1"));

        Node peer = node("n1", Protocol.of(pingReply));

        assertEquals(Protocol.VERSION, peer.getProtocolVersion());
        assertTrue(NodeCapabilities.capableOf(Feature.WASM_TASKS, List.of(peer)).size() == 1);
    }

    @Test
    void apingReplyFromAnOlderBuildCarriesNoVersion() {
        // What an actual 1.2.3 node sends: every other field, and no PROTOCOL.
        JSONObject fromOlder = new JSONObject().put("HOSTNAME", "n2").put("TASK_LIMIT", 4);

        Node peer = node("n2", Protocol.of(fromOlder));

        assertEquals(Protocol.UNKNOWN, peer.getProtocolVersion());
        assertFalse(NodeCapabilities.capableOf(Feature.WASM_TASKS, List.of(peer)).size() == 1);
    }
}
