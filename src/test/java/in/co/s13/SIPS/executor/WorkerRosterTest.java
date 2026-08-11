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

import in.co.s13.sips.lib.ml.ShardPlan;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Turning what dialled-in workers said about themselves into scheduling.
 *
 * <p>The open seam this closes: {@code WorkerConnections} stores each phone's
 * hello announcement as raw JSON, {@code WorkerEligibility} can judge a
 * reading, and {@code ShardPlan} can weight by measured speed — but nothing
 * connected them. Without the roster, a master either schedules every
 * dialled-in worker blindly or hand-parses announcements at every call site,
 * and hand-parsed trust decisions drift.
 *
 * <p>The announcements are made by the devices themselves, which is the point
 * and the danger: a phone is the only party that knows its battery, and also
 * a party with every incentive to be believed. So parsing fails closed — a
 * worker whose announcement is missing, malformed, or silent about power is
 * not scheduled, for the same reason an unreadable battery is not assumed
 * healthy.
 */
class WorkerRosterTest {

    private static JSONObject healthy(double benchMs) {
        return new JSONObject()
                .put("MAINS", false)
                .put("BATTERY", 80)
                .put("TEMPERATURE_C", 28.0)
                .put("BENCH_MS", new org.json.JSONArray(List.of(500L,
                        (long) benchMs, (long) benchMs, (long) benchMs)));
    }

    /**
     * {@link #healthy} without a Celsius reading — the shape an iOS
     * announcement actually has, since TEMPERATURE_C outranks THERMAL_STATE
     * by design and would otherwise silently mask it in a test.
     */
    private static JSONObject healthyNoCelsius(double benchMs) {
        JSONObject announcement = healthy(benchMs);
        announcement.remove("TEMPERATURE_C");
        return announcement;
    }

    @Test
    void aHealthyAnnouncedWorkerIsSchedulable() {
        WorkerRoster roster = WorkerRoster.from(Map.of("phone-1", healthy(20)));

        assertEquals(List.of("phone-1"), roster.eligible());
    }

    @Test
    void aLowBatteryWorkerIsLeftOut() {
        WorkerRoster roster = WorkerRoster.from(Map.of(
                "phone-1", healthy(20),
                "phone-2", healthy(20).put("BATTERY", 12)));

        assertEquals(List.of("phone-1"), roster.eligible());
        assertTrue(roster.refusalOf("phone-2").orElseThrow().contains("battery"),
                roster.refusalOf("phone-2").orElseThrow());
    }

    @Test
    void aHotWorkerIsLeftOut() {
        WorkerRoster roster = WorkerRoster.from(Map.of(
                "phone-1", healthy(20).put("TEMPERATURE_C", 52.0)));

        assertTrue(roster.eligible().isEmpty());
    }

    @Test
    void anAnnouncementSilentAboutPowerFailsClosed() {
        // The device that says nothing about its battery is exactly the one
        // whose battery should not be assumed healthy. Same rule as
        // WorkerEligibility, applied at the parsing boundary.
        WorkerRoster roster = WorkerRoster.from(Map.of(
                "phone-1", new JSONObject().put("BENCH_MS",
                        new org.json.JSONArray(List.of(500L, 20L, 20L, 20L)))));

        assertTrue(roster.eligible().isEmpty());
        assertTrue(roster.refusalOf("phone-1").orElseThrow().contains("power"),
                roster.refusalOf("phone-1").orElseThrow());
    }

    @Test
    void aGarbageAnnouncementFailsClosedRatherThanThrows() {
        // One phone sending nonsense must not take the roster down with it;
        // it is simply not scheduled, with the reason kept.
        WorkerRoster roster = WorkerRoster.from(Map.of(
                "phone-1", healthy(20),
                "phone-2", new JSONObject().put("BATTERY", "eighty")));

        assertEquals(List.of("phone-1"), roster.eligible());
        assertTrue(roster.refusalOf("phone-2").isPresent());
    }

    @Test
    void aMainsWorkerNeedsNoBattery() {
        WorkerRoster roster = WorkerRoster.from(Map.of(
                "laptop-1", new JSONObject().put("MAINS", true)
                        .put("BENCH_MS", new org.json.JSONArray(
                                List.of(500L, 10L, 10L, 10L)))));

        assertEquals(List.of("laptop-1"), roster.eligible());
    }

    @Test
    void measuredSpeedsFeedTheShardPlanner() {
        // The whole point of the bench: a worker twice as fast gets twice the
        // rows, from its own measured timings rather than its model name.
        WorkerRoster roster = WorkerRoster.from(Map.of(
                "fast", healthy(10),
                "slow", healthy(20)));

        Map<String, ShardPlan.Measured> speeds = roster.speeds();
        List<ShardPlan.Shard> shards = ShardPlan.acrossMeasured(3000, speeds);

        Map<String, Long> byWorker = new java.util.HashMap<>();
        shards.forEach(shard -> byWorker.put(shard.nodeUuid(), shard.sampleCount()));
        assertEquals(2000L, byWorker.get("fast"));
        assertEquals(1000L, byWorker.get("slow"));
    }

    @Test
    void aWorkerWithoutTimingsIsEligibleButAverage() {
        // Fitness and speed are separate questions. A healthy worker that
        // sent no bench yet gets work -- at the planner's unmeasured default,
        // not starved. Treating unknown as slow would starve every machine
        // not yet benchmarked, which is every new machine.
        WorkerRoster roster = WorkerRoster.from(Map.of(
                "phone-1", healthy(20).put("BENCH_MS", new org.json.JSONArray())));

        assertEquals(List.of("phone-1"), roster.eligible());
        assertTrue(roster.speeds().get("phone-1").weight() > 0);
    }

    @Test
    void hostileTimingsAreTreatedAsNoTimings() {
        // A worker claiming zero-millisecond runs claims infinite speed and
        // would be handed the whole dataset. It keeps its eligibility and
        // loses its claim.
        WorkerRoster roster = WorkerRoster.from(Map.of(
                "liar", healthy(20).put("BENCH_MS",
                        new org.json.JSONArray(List.of(500L, 0L, 0L, 0L))),
                "honest", healthy(20)));

        Map<String, ShardPlan.Measured> speeds = roster.speeds();
        assertTrue(speeds.get("liar").weight() <= speeds.get("honest").weight(),
                "a claimed infinite speed must not out-rank a measured one");
    }

    @Test
    void aWorkerActivelyInUseIsLeftOutByDefault() {
        // JPPF's Idle Host mode, applied to a fleet of phones: an unqualified
        // "healthy" phone whose owner is mid-conversation with it should not
        // be handed an hour of training.
        WorkerRoster roster = WorkerRoster.from(Map.of(
                "phone-1", healthy(20),
                "phone-2", healthy(20).put("IN_USE", true)));

        assertEquals(List.of("phone-1"), roster.eligible());
        assertTrue(roster.refusalOf("phone-2").orElseThrow().contains("use"),
                roster.refusalOf("phone-2").orElseThrow());
    }

    @Test
    void aWorkerSilentAboutActiveUseIsNotPenalisedForIt() {
        // Unlike battery: hardly any platform can report this at all, so
        // treating silence as refusal would refuse nearly the whole fleet.
        // healthy() never sets IN_USE, and every other test here relies on
        // that not costing eligibility -- this just says so explicitly.
        WorkerRoster roster = WorkerRoster.from(Map.of("phone-1", healthy(20)));

        assertEquals(List.of("phone-1"), roster.eligible());
    }

    @Test
    void aDeviceReportingOnlyAThermalStateIsJudgedByIt() {
        // iOS has no Celsius API at all -- ProcessInfo.thermalState is a
        // four-level enum, not a temperature -- so the roster must accept an
        // announcement that names a level instead of a degree.
        WorkerRoster roster = WorkerRoster.from(Map.of(
                "iphone-cool", healthyNoCelsius(20).put("THERMAL_STATE", "NOMINAL"),
                "iphone-hot", healthyNoCelsius(20).put("THERMAL_STATE", "SERIOUS")));

        assertEquals(List.of("iphone-cool"), roster.eligible());
        assertTrue(roster.refusalOf("iphone-hot").orElseThrow().toLowerCase()
                .contains("thermal"), roster.refusalOf("iphone-hot").orElseThrow());
    }

    @Test
    void aTemperatureCFieldTakesPriorityOverAThermalState() {
        // Should not happen from one honest platform, but if both arrive the
        // numeric reading is more informative and wins -- matching
        // WorkerEligibility's own precedence. healthy() already sets a safe
        // TEMPERATURE_C, so a CRITICAL THERMAL_STATE alongside it must be
        // ignored rather than refusing the worker.
        WorkerRoster roster = WorkerRoster.from(Map.of(
                "phone-1", healthy(20).put("THERMAL_STATE", "CRITICAL")));

        assertEquals(List.of("phone-1"), roster.eligible());
    }

    @Test
    void anUnrecognisedThermalStateNameFailsClosed() {
        WorkerRoster roster = WorkerRoster.from(Map.of(
                "phone-1", healthyNoCelsius(20).put("THERMAL_STATE", "MOLTEN")));

        assertTrue(roster.eligible().isEmpty());
        assertTrue(roster.refusalOf("phone-1").isPresent());
    }

    @Test
    void anEmptyRosterSaysSoUsefully() {
        WorkerRoster roster = WorkerRoster.from(Map.of());

        assertTrue(roster.eligible().isEmpty());
        assertThrows(IllegalStateException.class, roster::requireWorkers);
    }
}
