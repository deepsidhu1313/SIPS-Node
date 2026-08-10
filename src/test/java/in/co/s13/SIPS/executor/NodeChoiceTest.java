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
import in.co.s13.sips.scheduler.ClusterState;
import in.co.s13.sips.scheduler.ReadyTask;
import in.co.s13.sips.schedulers.placement.EarliestFinish;
import in.co.s13.sips.schedulers.placement.LeastLoaded;
import java.util.List;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Showing the live cluster to a placement policy.
 *
 * <p>This is the bridge that decides whether a policy is a decision or a
 * setting. Get the translation wrong — treat an unbenchmarked node as slow, let
 * map order decide a tie — and every policy above it inherits the mistake while
 * appearing to work.
 */
class NodeChoiceTest {

    /**
     * A node with a benchmark score and a queue.
     *
     * <p>Built through the real {@link LiveNode} so the bridge is reading the
     * same shape the cluster actually produces, benchmark JSON included.
     */
    private static Node node(String uuid, double cpuScore, int queued) {
        JSONObject benchmarks = cpuScore == 0
                ? new JSONObject()
                : new JSONObject().put("CPU", new JSONObject().put("Benchmarks",
                        new JSONObject().put("Composite Score", cpuScore)));
        return new LiveNode(uuid, "host-" + uuid, "linux", "cpu", 4, queued,
                8L << 30, 4L << 30, 512L << 30, 256L << 30, benchmarks,
                System.currentTimeMillis(), 0.1);
    }

    @Test
    void aFasterNodeCostsLessForTheSameWork() {
        List<Node> nodes = List.of(node("fast", 4.0, 0), node("slow", 1.0, 0));

        ReadyTask task = NodeChoice.describe("thumbnail", 100, nodes);

        assertEquals(25, task.costOn("fast"), 0.001);
        assertEquals(100, task.costOn("slow"), 0.001);
    }

    @Test
    void anUnbenchmarkedNodeIsBaselineNotSlow() {
        // Treating unknown as slow would quietly exclude every machine that has
        // not been measured yet -- which is every new machine.
        assertEquals(NodeChoice.UNMEASURED_SPEED, NodeChoice.speedOf(node("new", 0, 0)), 0.001);
        assertEquals(NodeChoice.UNMEASURED_SPEED, NodeChoice.speedOf(node("odd", -5, 0)), 0.001);
    }

    @Test
    void aNodesQueueBecomesWhenItIsAvailable() {
        ClusterState state = NodeChoice.stateOf(
                List.of(node("busy", 1.0, 7), node("idle", 1.0, 0)));

        assertEquals(7, state.availableAt("busy"), 0.001);
        assertEquals(0, state.availableAt("idle"), 0.001);
    }

    @Test
    void nodesAreOfferedInAStableOrder() {
        // Two equally good nodes must break the tie the same way every run;
        // otherwise the same job schedules differently for no visible reason.
        List<Node> shuffled = List.of(node("zebra", 1.0, 0), node("alpha", 1.0, 0));

        assertEquals(List.of("alpha", "zebra"),
                List.copyOf(NodeChoice.stateOf(shuffled).nodes()));
    }

    @Test
    void thePolicyActuallyDecides() {
        // The point of the bridge. Same cluster, two policies, two answers:
        // "fast" is busy but four times quicker, so it still finishes first.
        List<Node> nodes = List.of(node("fast", 4.0, 3), node("slow", 1.0, 0));
        ReadyTask task = NodeChoice.describe("work", 100, nodes);

        assertEquals("fast",
                NodeChoice.place(new EarliestFinish(), task, nodes).orElseThrow());
        assertEquals("slow",
                NodeChoice.place(new LeastLoaded(), task, nodes).orElseThrow());
    }

    @Test
    void anEmptyClusterPlacesNothingRatherThanThrowing() {
        // Nobody to run it is a normal state on a cluster starting up, not a
        // programming error.
        ReadyTask task = ReadyTask.named("work").cost(1).build();

        assertTrue(NodeChoice.place(new EarliestFinish(), task, List.of()).isEmpty());
        assertTrue(NodeChoice.place(new EarliestFinish(), task, null).isEmpty());
    }

    @Test
    void aMissingPolicyIsAProgrammingError() {
        assertThrows(IllegalArgumentException.class, () -> NodeChoice.place(
                null, ReadyTask.named("w").cost(1).build(), List.of(node("a", 1, 0))));
    }

    @Test
    void unmeasuredPowerIsZeroRatherThanAttractive() {
        // An energy-aware policy must not send everything to the node nobody
        // measured.
        assertEquals(0, NodeChoice.stateOf(List.of(node("a", 1.0, 0))).wattsOf("a"), 0.001);
    }
}
