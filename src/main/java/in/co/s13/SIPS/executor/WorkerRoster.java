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

import in.co.s13.sips.lib.array.WorkerBench;
import in.co.s13.sips.lib.ml.ShardPlan;
import in.co.s13.sips.lib.ml.WorkerEligibility;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * What the master decides about dialled-in workers, from what they said.
 *
 * <p>This closes the seam between three pieces that existed separately:
 * {@code WorkerConnections} holds each worker's hello announcement as raw
 * JSON, {@link WorkerEligibility} judges a reading, and {@link ShardPlan}
 * weights by measured speed. The roster parses every announcement once,
 * applies the policy, and hands the planner exactly the workers worth
 * scheduling — so trust decisions live in one place instead of drifting
 * across call sites.
 *
 * <h2>Announcements are claims</h2>
 *
 * <p>The device is the only party that knows its battery — and a party with
 * every incentive to be believed. Two rules follow. Parsing <b>fails
 * closed</b>: an announcement that is malformed or silent about power leaves
 * that worker unscheduled with the reason kept, because the device that
 * cannot say how it is doing is the one not to assume healthy. And claimed
 * speeds are <b>bounded by refusal</b>: hostile timings (a zero-millisecond
 * run, claiming infinite speed) are treated as no timings at all — the worker
 * keeps its eligibility and loses its claim, landing at the planner's
 * unmeasured default rather than being handed the whole dataset.
 *
 * <h2>The announcement schema</h2>
 *
 * <pre>
 * MAINS          boolean — on wall power
 * BATTERY        int     — percent, if on battery
 * TEMPERATURE_C  double  — optional, wins over THERMAL_STATE if both arrive
 * THERMAL_STATE  string  — optional; NOMINAL/FAIR/SERIOUS/CRITICAL, iOS's
 *                          vocabulary for a platform with no Celsius API
 * BENCH_MS       [long]  — WorkerBench.standard() timings, warm-up first
 * IN_USE         boolean — optional; whether the owner is using it right now
 * </pre>
 */
public final class WorkerRoster {

    private final Map<String, ShardPlan.Measured> speeds = new LinkedHashMap<>();
    private final Map<String, String> refusals = new LinkedHashMap<>();

    private WorkerRoster() {
    }

    /** Builds a roster from each worker's announcement, judging as it parses. */
    public static WorkerRoster from(Map<String, JSONObject> announcements) {
        WorkerRoster roster = new WorkerRoster();
        // Sorted, so two masters reading the same fleet produce the same
        // roster order -- and with it the same shard division.
        for (Map.Entry<String, JSONObject> entry
                : new TreeMap<>(announcements).entrySet()) {
            roster.admit(entry.getKey(), entry.getValue());
        }
        return roster;
    }

    private void admit(String workerId, JSONObject announcement) {
        WorkerEligibility.Reading reading;
        try {
            reading = readingOf(announcement);
        } catch (RuntimeException malformed) {
            // One phone sending nonsense must not take the roster down; it is
            // simply not scheduled, and the reason is kept for the log.
            refusals.put(workerId, "unreadable announcement: " + malformed.getMessage());
            return;
        }
        WorkerEligibility.Report report =
                WorkerEligibility.of(reading, WorkerEligibility.Policy.defaults());
        if (!report.fit()) {
            refusals.put(workerId, report.refusal().orElse("unfit"));
            return;
        }
        speeds.put(workerId, speedOf(announcement));
    }

    /**
     * The announcement as the reading the eligibility rules judge.
     *
     * <p>Temperature has three shapes, tried in order: a real {@code
     * TEMPERATURE_C} wins when present, since a number is more informative
     * than a bucket; {@code THERMAL_STATE} — iOS's four-level vocabulary,
     * since {@code ProcessInfo.thermalState} is all a Swift worker can ever
     * report — is tried next; a device reporting neither falls back to the
     * existing unknown-temperature handling.
     */
    private static WorkerEligibility.Reading readingOf(JSONObject announcement) {
        double temperature = announcement.optDouble("TEMPERATURE_C", Double.NaN);
        boolean mains = announcement.optBoolean("MAINS", false);

        WorkerEligibility.Reading reading;
        if (!Double.isNaN(temperature)) {
            reading = mains ? WorkerEligibility.Reading.mains(temperature)
                    : WorkerEligibility.Reading.onBattery(requiredBatteryPercent(announcement),
                            temperature);
        } else if (announcement.has("THERMAL_STATE")) {
            // valueOf() throws IllegalArgumentException on an unrecognised
            // name, which the caller already treats as a failed-closed
            // refusal -- exactly right for a level this framework does not
            // understand rather than one it should trust blindly.
            WorkerEligibility.ThermalLevel level = WorkerEligibility.ThermalLevel
                    .valueOf(announcement.getString("THERMAL_STATE"));
            reading = mains ? WorkerEligibility.Reading.mainsWithThermalLevel(level)
                    : WorkerEligibility.Reading.onBatteryWithThermalLevel(
                            requiredBatteryPercent(announcement), level);
        } else {
            reading = mains ? WorkerEligibility.Reading.unknownTemperature()
                    : WorkerEligibility.Reading.onBattery(requiredBatteryPercent(announcement), 25.0);
        }

        // Unlike power, silence here is not suspicious: hardly any platform
        // can report active use at all, so IN_USE absent leaves the reading
        // as-is (unknown), and only an explicit true or false is applied.
        if (announcement.has("IN_USE")) {
            reading = announcement.getBoolean("IN_USE")
                    ? reading.activelyInUse() : reading.confirmedIdle();
        }
        return reading;
    }

    /** Fails closed: silent about power is not "probably fine". */
    private static int requiredBatteryPercent(JSONObject announcement) {
        if (!announcement.has("BATTERY")) {
            throw new IllegalArgumentException("the announcement says nothing about power");
        }
        return announcement.getInt("BATTERY");
    }

    /** Claimed timings, believed only as far as they are believable. */
    private static ShardPlan.Measured speedOf(JSONObject announcement) {
        JSONArray timings = announcement.optJSONArray("BENCH_MS");
        if (timings == null || timings.length() < WorkerBench.MINIMUM_TIMED_RUNS + 1) {
            // No measurement yet: the planner's unmeasured default, not
            // starvation -- every new machine starts unmeasured.
            return new ShardPlan.Measured(0, 0);
        }
        long[] millis = new long[timings.length()];
        for (int i = 0; i < timings.length(); i++) {
            millis[i] = timings.getLong(i);
        }
        try {
            return WorkerBench.measured(millis);
        } catch (IllegalArgumentException hostile) {
            // A zero-millisecond run claims infinite speed and would be
            // handed the whole dataset. Eligibility kept, claim refused.
            return new ShardPlan.Measured(0, 0);
        }
    }

    /** The workers worth giving work to, in stable order. */
    public List<String> eligible() {
        return new ArrayList<>(speeds.keySet());
    }

    /** Measured speeds for {@link ShardPlan#acrossMeasured}. */
    public Map<String, ShardPlan.Measured> speeds() {
        return Map.copyOf(speeds);
    }

    /** Why a worker was left out, if it was. */
    public Optional<String> refusalOf(String workerId) {
        return Optional.ofNullable(refusals.get(workerId));
    }

    /** Fails now, with the refusals, rather than scheduling onto nobody. */
    public void requireWorkers() {
        if (speeds.isEmpty()) {
            throw new IllegalStateException("No dialled-in worker is fit for work; "
                    + "refused: " + refusals);
        }
    }
}
