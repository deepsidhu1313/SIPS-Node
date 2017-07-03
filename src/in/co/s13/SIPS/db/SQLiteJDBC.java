/* 
 * Copyright (C) 2017 Navdeep Singh Sidhu
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

/**
 *
 * @author Nika
 */
import in.co.s13.SIPS.settings.Settings;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SQLiteJDBC {

    Connection c = null;
    Statement stmt = null;
    ResultSet rs = null;

    public SQLiteJDBC() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(SQLiteJDBC.class.getName()).log(Level.SEVERE, null, ex);
        }
      }

    public void closeConnection() {
        try {
            stmt.close();
            c.close();
        } catch (SQLException ex) {
            try {
                c.close();
            } catch (SQLException ex1) {
                Logger.getLogger(SQLiteJDBC.class.getName()).log(Level.SEVERE, null, ex1);
            }
            Logger.getLogger(SQLiteJDBC.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void createtable(String db, String sql) {
        try {
            c = DriverManager.getConnection("jdbc:sqlite:" + db);
            //System.out.println("Opened database successfully");

            stmt = c.createStatement();
            stmt.executeUpdate(sql);
            // stmt.close();
            // c.close();
            System.out.println(sql);
            System.out.println("Table created successfully on DB " + db);

        } catch (SQLException e) {
            System.out.println(sql+" didnot executed on "+db);
            System.out.println(sql+" didnot executed on "+db);
            System.out.println(e.getClass().getName() + ": " + e.getMessage());

        }

    }

    public void insert(String db, String sql) {
        try {
            c = DriverManager.getConnection("jdbc:sqlite:" + db);
            c.setAutoCommit(false);
            //  System.out.println("Opened database successfully");

            stmt = c.createStatement();
            stmt.executeUpdate(sql);

            //    stmt.close();
            c.commit();
            //    c.close();
            System.out.println(sql);
            System.out.println("Records created successfully on DB " + db);

        } catch (SQLException e) {
            System.out.println(sql+" didnot executed on "+db);
            System.out.println(sql+" didnot executed on "+db);
            System.out.println(e.getClass().getName() + ": " + e.getMessage());

        }
    }

    public ResultSet select(String db, String sql)  {

        ResultSet rs2 = null;
        try {
            c = DriverManager.getConnection("jdbc:sqlite:" + db);
            c.setAutoCommit(false);
            //  System.out.println("Opened database successfully");

            stmt = c.createStatement();
            rs2 = stmt.executeQuery(sql);
            System.out.println(sql);
            System.out.println("Select Operation done successfully on DB " + db);
        } catch (SQLException e) {
            System.out.println(sql+" didnot executed on "+db);
            System.out.println(sql+" didnot executed on "+db);
            System.out.println(e.getClass().getName() + ": " + e.getMessage());

        }

        return rs2;

    }

    public void Update(String db, String sql) {
        try {
            c = DriverManager.getConnection("jdbc:sqlite:" + db);
            c.setAutoCommit(false);

            stmt = c.createStatement();
            int r=stmt.executeUpdate(sql);
            c.commit();

            //      stmt.close();
            //    c.close();
            System.out.println(sql);
            System.out.println(r+" Rows effected Update Operation done successfully on DB " + db);
        } catch (SQLException e) {
            System.out.println(sql+" didnot executed on "+db);
            System.out.println(sql+" didnot executed on "+db);
            System.out.println(e.getClass().getName() + ": " + e.getMessage());

        }

    }

    public void Update(String db, String sql, Object obj) {
        try {
            c = DriverManager.getConnection("jdbc:sqlite:" + db);
            c.setAutoCommit(false);
            PreparedStatement ps = null;

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);

            oos.writeObject(obj);
            oos.flush();
            oos.close();
            bos.close();

            byte[] data = bos.toByteArray();

//            sql = "insert into javaobject (javaObject) values(?)";
            ps = c.prepareStatement(sql);
            ps.setObject(1, data);
            ps.executeUpdate();

            c.commit();

            System.out.println(sql);
            System.out.println("Update Operation done successfully on DB " + db);
        } catch (SQLException e) {
            System.out.println(sql+" didnot executed on "+db);
            System.out.println(sql+" didnot executed on "+db);
            System.out.println(e.getClass().getName() + ": " + e.getMessage());

        } catch (IOException ex) {
            System.out.println(sql+" didnot executed on "+db);
            System.out.println(sql+" didnot executed on "+db);
            Logger.getLogger(SQLiteJDBC.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    public void delete(String db, String sql) {
        try {
            c = DriverManager.getConnection("jdbc:sqlite:" + db);
            c.setAutoCommit(false);
            // System.out.println("Opened database successfully");

            stmt = c.createStatement();
            stmt.executeUpdate(sql);
            c.commit();

            // stmt.close();
            // c.close();
            System.out.println(sql);
            System.out.println("Delete Operation done successfully on DB " + db);
        } catch (SQLException e) {
            System.out.println(sql+" didnot executed on "+db);
            System.out.println(sql+" didnot executed on "+db);
            System.out.println(e.getClass().getName() + ": " + e.getMessage());

        }
    }

    public void execute(String db, String sql) {
        try {
          c = DriverManager.getConnection("jdbc:sqlite:" + db);
            c.setAutoCommit(false);
            // System.out.println("Opened database successfully");

            stmt = c.createStatement();
            stmt.executeUpdate(sql);
            c.commit();

            // stmt.close();
            // c.close();
            System.out.println(sql);
            System.out.println("Query Executed Operation done successfully on DB " + db);
        } catch (SQLException e) {
            System.out.println(sql+" didnot executed on "+db);
            System.out.println(sql+" didnot executed on "+db);
            System.out.println(e.getClass().getName() + ": " + e.getMessage());

        }

    }

    public void toFile(String db, String sql, String file) {
        try {
            ResultSet result = this.select(db, sql);
            ResultSetMetaData rsm = result.getMetaData();
            int columncount = rsm.getColumnCount();
            PrintStream out = new PrintStream(file); //new AppendFileStream
            for (int i = 1; i <= columncount; i++) {
                out.print(rsm.getColumnName(i) + "\t");
            }
            out.print("\n");
            while (result.next()) {
                for (int i = 1; i <= columncount; i++) {
                    out.print(result.getString(i) + "\t");
                }
                out.print("\n");

            }
            out.close();
        } catch (SQLException ex) {
            System.out.println(sql+" didnot executed on "+db);
            System.out.println(sql+" didnot executed on "+db);
            Logger.getLogger(SQLiteJDBC.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            System.out.println(sql+" didnot executed on "+db);
            System.out.println(sql+" didnot executed on "+db);
            Logger.getLogger(SQLiteJDBC.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static void main(String args[]) {
        SQLiteJDBC sqLiteJDBC = new SQLiteJDBC();
    }
}
