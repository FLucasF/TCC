package com.pousadamarealta.infrastructure;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class Database {
  private static HikariDataSource dataSource;

  public static void initialize(String dbUrl, String dbUser, String dbPassword) throws SQLException {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(dbUrl);
    config.setUsername(dbUser);
    config.setPassword(dbPassword);
    config.setMaximumPoolSize(10);
    config.setMinimumIdle(2);
    dataSource = new HikariDataSource(config);

    createSchema();
  }

  private static void createSchema() throws SQLException {
    try (Connection conn = dataSource.getConnection()) {
      conn.createStatement()
          .execute(
              "CREATE TABLE IF NOT EXISTS rooms ("
                  + "number INTEGER PRIMARY KEY,"
                  + "type VARCHAR(50) NOT NULL,"
                  + "capacity INTEGER NOT NULL,"
                  + "nightly_rate NUMERIC(10,2) NOT NULL"
                  + ")");

      conn.createStatement()
          .execute(
              "CREATE TABLE IF NOT EXISTS reservations ("
                  + "id SERIAL PRIMARY KEY,"
                  + "room_number INTEGER NOT NULL REFERENCES rooms(number),"
                  + "guest_name VARCHAR(255) NOT NULL,"
                  + "guest_email VARCHAR(255) NOT NULL,"
                  + "guest_cpf VARCHAR(11) NOT NULL,"
                  + "check_in DATE NOT NULL,"
                  + "check_out DATE NOT NULL,"
                  + "guests INTEGER NOT NULL,"
                  + "rate_plan VARCHAR(50) NOT NULL,"
                  + "nights INTEGER NOT NULL,"
                  + "subtotal NUMERIC(10,2) NOT NULL,"
                  + "discount NUMERIC(10,2) NOT NULL,"
                  + "total NUMERIC(10,2) NOT NULL,"
                  + "status VARCHAR(50) NOT NULL,"
                  + "amount_paid NUMERIC(10,2) NOT NULL DEFAULT 0,"
                  + "refund NUMERIC(10,2)"
                  + ")");

      conn.createStatement()
          .execute(
              "CREATE TABLE IF NOT EXISTS messages ("
                  + "id SERIAL PRIMARY KEY,"
                  + "channel VARCHAR(50) NOT NULL,"
                  + "to_address VARCHAR(255) NOT NULL,"
                  + "subject VARCHAR(255) NOT NULL,"
                  + "body TEXT NOT NULL"
                  + ")");

      conn.createStatement()
          .execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_rooms ON rooms(number)");
      conn.createStatement()
          .execute(
              "CREATE INDEX IF NOT EXISTS idx_reservations_room_dates ON reservations(room_number, check_in, check_out)");
      conn.createStatement()
          .execute("CREATE INDEX IF NOT EXISTS idx_messages_to ON messages(to_address)");

      insertInitialRooms(conn);
    }
  }

  private static void insertInitialRooms(Connection conn) throws SQLException {
    try (var rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM rooms")) {
      rs.next();
      if (rs.getInt(1) == 0) {
        conn.createStatement()
            .execute(
                "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES "
                    + "(101, 'STANDARD', 2, 249.90), "
                    + "(102, 'STANDARD', 2, 249.90), "
                    + "(201, 'DELUXE', 3, 389.90), "
                    + "(202, 'DELUXE', 3, 389.90), "
                    + "(301, 'SUITE', 4, 579.90)");
      }
    }
  }

  public static Connection getConnection() throws SQLException {
    return dataSource.getConnection();
  }

  public static void close() {
    if (dataSource != null) {
      dataSource.close();
    }
  }
}
