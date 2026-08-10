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
import in.co.s13.sips.scheduler.ClusterState;
import in.co.s13.sips.scheduler.PlacementPolicy;
import in.co.s13.sips.scheduler.ReadyTask;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Turns the live cluster into the terms a {@link PlacementPolicy} reasons about,
 * and asks it where a task should go.
 *
 * <p>The bridge that makes a placement policy a decision rather than a setting.
 * A policy is written against estimates and availability; the cluster keeps
 * benchmark scores and queue lengths. Somebody has to translate, and doing it
 * here means every policy sees the same view rather than each reaching into node
 * internals its own way.
 *
 * <p>A node with no benchmark is given the baseline rather than being excluded:
 * a cluster where one machine has not been measured yet should still schedule.
 */
public final class NodeChoice {

    /** The speed a node with no benchmark is assumed to have. */
    public static final double UNMEASURED_SPEED = 1.0;

    private NodeChoice() {
    }

    /**
     * Where a task should run.
     *
     * @return the chosen node's uuid, or empty when the policy declined or there
     *         is nobody to run it
     */
    public static Optional<String> place(PlacementPolicy policy, ReadyTask task,
            Collection<Node> nodes) {
        if (policy == null) {
            throw new IllegalArgumentException("policy must not be null");
        }
        if (nodes == null || nodes.isEmpty()) {
            return Optional.empty();
        }
        return policy.place(task, stateOf(nodes));
    }

    /**
     * Describes a piece of work in the policy's terms.
     *
     * @param baseCost what it costs on a baseline node; scaled per node by that
     *        node's measured speed
     */
    public static ReadyTask describe(String name, double baseCost, Collection<Node> nodes) {
        ReadyTask.Builder task = ReadyTask.named(name).cost(baseCost);
        for (Node node : nodes) {
            task.costOn(node.getUuid(), baseCost / speedOf(node));
        }
        return task.build();
    }

    /** The cluster as a policy sees it: who is there and when they are free. */
    public static ClusterState stateOf(Collection<Node> nodes) {
        Map<String, Double> watts = new LinkedHashMap<>();
        // Sorted uuids so a tie between equally good nodes breaks the same way
        // every time rather than following map iteration order.
        ClusterState state = ClusterState.idle(
                nodes.stream().map(Node::getUuid).sorted().toList());
        for (Node node : nodes) {
            state = state.busyUntil(node.getUuid(), queueDepthOf(node));
            watts.put(node.getUuid(), 0.0);
        }
        return state.withWatts(watts);
    }

    /**
     * A node's speed relative to the baseline, from its benchmark.
     *
     * <p>A missing or nonsensical score means unmeasured, not slow — treating it
     * as slow would quietly exclude every machine that had not been benchmarked
     * yet, which is every new machine.
     */
    static double speedOf(Node node) {
        try {
            double score = node.getCPUScore();
            return score > 0 ? score : UNMEASURED_SPEED;
        } catch (RuntimeException ex) {
            return UNMEASURED_SPEED;
        }
    }

    /**
     * How long a node is already committed for, as the policy's "available at".
     *
     * <p>Queue length stands in for time: the node reports how much it is
     * holding, not how long it will take.
     */
    static double queueDepthOf(Node node) {
        try {
            return Math.max(0, node.getWaiting_in_que());
        } catch (RuntimeException ex) {
            return 0;
        }
    }
}
