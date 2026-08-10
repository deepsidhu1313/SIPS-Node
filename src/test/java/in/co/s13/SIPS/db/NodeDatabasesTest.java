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

import in.co.s13.sips.lib.db.Migration;
import in.co.s13.sips.lib.db.Migrator;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The node's long-lived warehouses across a restart.
 *
 * <p>What is being replaced was not a schema mistake but a data one: the create
 * failed on every start after the first, and the code took that as a cue to
 * rename the existing warehouse away and begin an empty one. So the history it
 * exists to accumulate never accumulated.
 */
class NodeDatabasesTest {

    private static List<String> columnsOf(String database, String table) throws SQLException {
        List<String> columns = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
                Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rows.next()) {
                columns.add(rows.getString("name"));
            }
        }
        return columns;
    }

    private static int rowsIn(String database, String table) throws SQLException {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
                Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery("SELECT COUNT(*) AS n FROM " + table)) {
            return rows.next() ? rows.getInt("n") : -1;
        }
    }

    private static void run(String database, Migration migration) {
        new Migrator().with(migration).migrate(database);
    }

    @Test
    void theDistributionWarehouseHasEveryColumnItsInsertsWrite(@TempDir Path dir)
            throws SQLException {
        String database = dir.resolve("dw-dist.db").toString();

        run(database, NodeDatabases.createDistributionWarehouse());

        List<String> columns = columnsOf(database, "DISTWH");
        assertEquals(34, columns.size(), columns.toString());
        assertTrue(columns.contains("EXITCODE"));
        assertTrue(columns.contains("cacheMisses"));
    }

    @Test
    void theResultWarehouseHasEveryColumnItsInsertsWrite(@TempDir Path dir) throws SQLException {
        String database = dir.resolve("dw-result.db").toString();

        run(database, NodeDatabases.createResultWarehouse());

        List<String> columns = columnsOf(database, "RESULTWH");
        assertEquals(29, columns.size(), columns.toString());
        assertTrue(columns.contains("schedulingOH"));
        assertTrue(columns.contains("duplicates"));
    }

    @Test
    void restartingKeepsWhatTheWarehouseAlreadyHeld(@TempDir Path dir) throws SQLException {
        // The bug this replaces, stated as a test: a second start used to
        // rename the file away and begin an empty one.
        String database = dir.resolve("dw-dist.db").toString();
        run(database, NodeDatabases.createDistributionWarehouse());
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
                Statement statement = connection.createStatement()) {
            statement.execute(
                    "INSERT INTO DISTWH (IP, PROJECT) VALUES ('10.0.0.1', 'mandelbrot')");
        }

        run(database, NodeDatabases.createDistributionWarehouse());
        run(database, NodeDatabases.createDistributionWarehouse());

        assertEquals(1, rowsIn(database, "DISTWH"),
                "the row written before the restart should still be there");
    }

    @Test
    void aWarehouseFromAnEarlierReleaseIsAdoptedNotRebuilt(@TempDir Path dir)
            throws SQLException {
        // An existing installation already has DISTWH but no migration ledger.
        // It must be taken over as it stands, history included.
        String database = dir.resolve("dw-dist.db").toString();
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE DISTWH (ID INTEGER PRIMARY KEY AUTOINCREMENT "
                    + "NOT NULL, IP TEXT NOT NULL, PROJECT TEXT)");
            statement.execute("INSERT INTO DISTWH (IP, PROJECT) VALUES ('10.0.0.9', 'older')");
        }

        run(database, NodeDatabases.createDistributionWarehouse());

        assertEquals(1, rowsIn(database, "DISTWH"), "the old history must survive the upgrade");
        assertEquals(List.of("2026_08_10_000001_create_distribution_warehouse"),
                Migrator.appliedTo(database));
    }

    @Test
    void bothWarehousesAreNamedWithThisPlatformsSeparator() {
        char foreign = java.io.File.separatorChar == '/' ? '\\' : '/';

        assertEquals(-1, NodeDatabases.DISTRIBUTION_WAREHOUSE.indexOf(foreign));
        assertEquals(-1, NodeDatabases.RESULT_WAREHOUSE.indexOf(foreign));
    }
}
