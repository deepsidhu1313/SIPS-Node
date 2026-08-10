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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps a shard on the node that already has its data.
 *
 * <p>An iterative job runs the same shards round after round. Round one's
 * worker 3 reads shard 3 of the dataset and pulls it across the network; if
 * round two's worker 3 lands somewhere else, it pulls the same shard again —
 * every round, for every shard that moved. On a dataset worth distributing that
 * transfer dwarfs the round's compute.
 *
 * <p>{@code NearestData} already knows how to prefer a node holding a task's
 * inputs, but it can only prefer what it has been told about. This is the
 * record: which node ran which shard, so the next round can ask.
 *
 * <p>A preference, never a requirement. If the remembered node is gone, busy
 * or too old to run the work, the scheduler places the shard elsewhere and the
 * data moves — slower for that round, and correct. Pinning work to a node that
 * cannot take it would trade a transfer for a stall.
 */
public final class ShardAffinity {

    /** Job token to shard to the node that last ran it. */
    private static final ConcurrentHashMap<String, Map<Integer, String>> PLACEMENTS =
            new ConcurrentHashMap<>();

    private ShardAffinity() {
    }

    /** Remembers where a shard ran. */
    public static void record(String jobToken, int shard, String nodeUuid) {
        if (jobToken == null || nodeUuid == null || shard < 0) {
            return;
        }
        PLACEMENTS.computeIfAbsent(jobToken, token -> new ConcurrentHashMap<>())
                .put(shard, nodeUuid);
    }

    /** Where a shard ran last, if it has run and that node is still usable. */
    public static Optional<String> preferredNode(String jobToken, int shard,
            Map<String, Node> usableNodes) {
        Map<Integer, String> placements = PLACEMENTS.get(jobToken);
        if (placements == null || usableNodes == null) {
            return Optional.empty();
        }
        String remembered = placements.get(shard);
        // Checked against the nodes that can actually take the work: a node that
        // has since died, or that a capability filter has excluded, is not a
        // preference worth honouring.
        return remembered != null && usableNodes.containsKey(remembered)
                ? Optional.of(remembered)
                : Optional.empty();
    }

    /**
     * Reorders a stage's chunks onto the nodes that already hold their shards.
     *
     * <p>Applied after scheduling rather than instead of it: the scheduler still
     * decides how the work divides and which nodes take part. This only swaps
     * which of the chosen nodes gets which shard, so a policy's load balance
     * survives while the data stops moving.
     *
     * @param assignments shard to the node the scheduler chose
     * @return shard to the node it should actually run on
     */
    public static Map<Integer, String> apply(String jobToken, Map<Integer, String> assignments,
            Map<String, Node> usableNodes) {
        Map<Integer, String> placed = new LinkedHashMap<>(assignments);
        Map<Integer, String> placements = PLACEMENTS.get(jobToken) == null
                ? Map.of() : new LinkedHashMap<>(PLACEMENTS.get(jobToken));
        if (placements.isEmpty()) {
            return placed;
        }

        // Only swaps within the chosen set, so each node keeps the same number
        // of chunks and the scheduler's balance is preserved exactly.
        List<Integer> shards = new ArrayList<>(placed.keySet());
        for (int shard : shards) {
            Optional<String> preferred = preferredNode(jobToken, shard, usableNodes);
            if (preferred.isEmpty() || preferred.get().equals(placed.get(shard))) {
                continue;
            }
            String wanted = preferred.get();
            Integer holder = null;
            for (int other : shards) {
                if (other != shard && wanted.equals(placed.get(other))) {
                    holder = other;
                    break;
                }
            }
            if (holder == null) {
                // The preferred node took no chunk this round; giving it one
                // would unbalance what the scheduler decided.
                continue;
            }
            placed.put(holder, placed.get(shard));
            placed.put(shard, wanted);
        }
        return placed;
    }

    /** How many shards of a job have a remembered home. */
    public static int remembered(String jobToken) {
        Map<Integer, String> placements = PLACEMENTS.get(jobToken);
        return placements == null ? 0 : placements.size();
    }

    /** Forgets a job, once it is over. */
    public static void forget(String jobToken) {
        PLACEMENTS.remove(jobToken);
    }
}
