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
package in.co.s13.SIPS.db;

import in.co.s13.sips.lib.common.SipsPaths;
import in.co.s13.sips.lib.db.Migration;
import in.co.s13.sips.lib.db.Migrator;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.function.Consumer;

/**
 * The node's long-lived databases, and the migrations that shape them.
 *
 * <p>These outlive a job and an upgrade, which is what makes them different
 * from the per-job AST databases — those are deleted and rebuilt on every
 * submission, so their schema can change freely.
 *
 * <h2>What this replaces</h2>
 *
 * <p>Both warehouses used to be created by a bare {@code CREATE TABLE} on the
 * first insert after startup, guarded by a static boolean. On a second start the
 * create failed, because the table was already there — and the code took that
 * failure as a cue to <em>rename the existing database out of the way</em> and
 * begin an empty one.
 *
 * <p>So every restart silently began a fresh warehouse and left the previous one
 * behind as {@code dw-dist-<timestamp>.db}. It reads like log rotation and is
 * not: it is the failure branch of a create that was never meant to fail.
 * Nothing depended on the accumulated history, which is presumably why nobody
 * noticed it never accumulated.
 */
public final class NodeDatabases {

    /** Per-chunk timings, one row per finished chunk. */
    public static final String DISTRIBUTION_WAREHOUSE =
            SipsPaths.join("log", "dw-dist.db");

    /** Per-job results, one row per finished job. */
    public static final String RESULT_WAREHOUSE =
            SipsPaths.join("log", "dw-result.db");

    private NodeDatabases() {
    }

    /**
     * Brings every long-lived database up to the schema this build expects.
     *
     * <p>Call at startup, every time. Against an up-to-date node it costs one
     * query per database and changes nothing, which is what lets it be
     * unconditional — there is no upgrade step for an operator to remember.
     */
    public static void migrate(Consumer<String> log) {
        new java.io.File("log").mkdirs();

        List<String> distribution = new Migrator()
                .with(createDistributionWarehouse())
                .logTo(log)
                .migrate(DISTRIBUTION_WAREHOUSE);

        List<String> results = new Migrator()
                .with(createResultWarehouse())
                .logTo(log)
                .migrate(RESULT_WAREHOUSE);

        if (!distribution.isEmpty() || !results.isEmpty()) {
            log.accept("Databases are up to date");
        }
    }

    /**
     * The distribution warehouse as it has always been.
     *
     * <p>Recorded as a migration rather than left implicit, so the next change
     * to it has somewhere to go. An existing warehouse already has this table,
     * and {@code IF NOT EXISTS} is what lets it be adopted rather than
     * destroyed and rebuilt.
     */
    static Migration createDistributionWarehouse() {
        return new Migration() {
            @Override
            public String id() {
                return "2026_08_10_000001_create_distribution_warehouse";
            }

            @Override
            public String description() {
                return "per-chunk timings";
            }

            @Override
            public void up(Connection database) throws SQLException {
                try (Statement statement = database.createStatement()) {
                    statement.execute("CREATE TABLE IF NOT EXISTS DISTWH ("
                            + "ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
                            + "IP TEXT NOT NULL,"
                            + "PROJECT TEXT,"
                            + "PID INT,"
                            + "CNO INT,"
                            + "VARTYPE INT,"
                            + "SCHEDULER INT,"
                            + "LStartTime LONG,"
                            + "LEndTime LONG,"
                            + "LExcTime LONG,"
                            + "CHUNKSIZE DECIMAL,"
                            + "LOWLIMIT DECIMAL,"
                            + "UPLIMIT DECIMAL,"
                            + "COUNTER DECIMAL,"
                            + "NExecutionTime LONG,"
                            + "NOH LONG,"
                            + "POH LONG,"
                            + "ENTERINQ LONG,"
                            + "STARTINQ LONG,"
                            + "WAITINQ LONG,"
                            + "SLEEPTIME LONG,"
                            + "PRFM DOUBLE,"
                            + "EXITCODE INT,"
                            + "avgCacheHitMissRatio DOUBLE,"
                            + "avgDownloadData LONG,"
                            + "avgDownloadSpeed DOUBLE,"
                            + "avgReqSent INT,"
                            + "avgUploadData LONG,"
                            + "avgUploadSpeed DOUBLE,"
                            + "avgReqRecieved INT,"
                            + "avgCachedData LONG,"
                            + "cacheHits TEXT,"
                            + "cacheMisses TEXT,"
                            + "TIMESTAMP DATE)");
                }
            }
        };
    }

    /** The result warehouse as it has always been. */
    static Migration createResultWarehouse() {
        return new Migration() {
            @Override
            public String id() {
                return "2026_08_10_000002_create_result_warehouse";
            }

            @Override
            public String description() {
                return "per-job results";
            }

            @Override
            public void up(Connection database) throws SQLException {
                try (Statement statement = database.createStatement()) {
                    statement.execute("CREATE TABLE IF NOT EXISTS RESULTWH ("
                            + "ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
                            + "PID TEXT,"
                            + "PROJECT TEXT,"
                            + "SCHEDULER TEXT,"
                            + "STARTTIME TEXT,"
                            + "ENDTIME TEXT,"
                            + "TOTALTIME TEXT,"
                            + "NOH TEXT,"
                            + "POH TEXT,"
                            + "CHUNKSIZE TEXT,"
                            + "TCHUNKS TEXT,"
                            + "TNODES TEXT,"
                            + "PRFM DOUBLE,"
                            + "FINISHED TEXT,"
                            + "AVGWAITINQ TEXT,"
                            + "AVGSLEEP TEXT,"
                            + "avgCacheHitMissRatio DOUBLE,"
                            + "avgDownloadData LONG,"
                            + "avgDownloadSpeed DOUBLE,"
                            + "avgReqSent INT,"
                            + "avgUploadData LONG,"
                            + "avgUploadSpeed DOUBLE,"
                            + "avgReqRecieved INT,"
                            + "avgCachedData LONG,"
                            + "selectedNodes INT,"
                            + "duplicates INT,"
                            + "schedulingOH LONG,"
                            + "distOH LONG,"
                            + "TIMESTAMP DATE)");
                }
            }
        };
    }
}
