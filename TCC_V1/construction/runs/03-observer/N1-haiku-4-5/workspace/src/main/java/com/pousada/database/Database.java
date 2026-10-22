package com.pousada.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
  private static HikariDataSource dataSource;

  public static void initialize() {
    String dbUrl = System.getenv("DB_URL");
    String dbUser = System.getenv("DB_USER");
    String dbPassword = System.getenv("DB_PASSWORD");

    if (dbUrl == null || dbUser == null || dbPassword == null) {
      throw new RuntimeException("Database environment variables not set");
    }

    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(dbUrl);
    config.setUsername(dbUser);
    config.setPassword(dbPassword);
    config.setMaximumPoolSize(10);

    dataSource = new HikariDataSource(config);

    createTables();
    insertInitialRooms();
  }

  public static Connection getConnection() throws SQLException {
    return dataSource.getConnection();
  }

  private static void createTables() {
    try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
      stmt.execute(
          "CREATE TABLE IF NOT EXISTS rooms ("
              + "number INT PRIMARY KEY, "
              + "type VARCHAR(20) NOT NULL, "
              + "capacity INT NOT NULL, "
              + "nightly_rate NUMERIC(10, 2) NOT NULL)");

      stmt.execute(
          "CREATE TABLE IF NOT EXISTS reservations ("
              + "id BIGSERIAL PRIMARY KEY, "
              + "room_number INT NOT NULL REFERENCES rooms(number), "
              + "guest_name VARCHAR(255) NOT NULL, "
              + "guest_email VARCHAR(255) NOT NULL, "
              + "guest_cpf VARCHAR(20) NOT NULL, "
              + "check_in DATE NOT NULL, "
              + "check_out DATE NOT NULL, "
              + "guests INT NOT NULL, "
              + "rate_plan VARCHAR(20) NOT NULL, "
              + "nights INT NOT NULL, "
              + "subtotal NUMERIC(10, 2) NOT NULL, "
              + "discount NUMERIC(10, 2) NOT NULL, "
              + "total NUMERIC(10, 2) NOT NULL, "
              + "status VARCHAR(20) NOT NULL, "
              + "amount_paid NUMERIC(10, 2) NOT NULL DEFAULT 0, "
              + "refund NUMERIC(10, 2))");

      stmt.execute(
          "CREATE TABLE IF NOT EXISTS messages ("
              + "id BIGSERIAL PRIMARY KEY, "
              + "channel VARCHAR(50) NOT NULL, "
              + "to_addr VARCHAR(255) NOT NULL, "
              + "subject VARCHAR(255) NOT NULL, "
              + "body TEXT NOT NULL, "
              + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

      conn.commit();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to create tables", e);
    }
  }

  private static void insertInitialRooms() {
    try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
      stmt.execute(
          "INSERT INTO rooms (number, type, capacity, nightly_rate) "
              + "SELECT 101, 'STANDARD', 2, 249.90 "
              + "WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE number = 101)");
      stmt.execute(
          "INSERT INTO rooms (number, type, capacity, nightly_rate) "
              + "SELECT 102, 'STANDARD', 2, 249.90 "
              + "WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE number = 102)");
      stmt.execute(
          "INSERT INTO rooms (number, type, capacity, nightly_rate) "
              + "SELECT 201, 'DELUXE', 3, 389.90 "
              + "WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE number = 201)");
      stmt.execute(
          "INSERT INTO rooms (number, type, capacity, nightly_rate) "
              + "SELECT 202, 'DELUXE', 3, 389.90 "
              + "WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE number = 202)");
      stmt.execute(
          "INSERT INTO rooms (number, type, capacity, nightly_rate) "
              + "SELECT 301, 'SUITE', 4, 579.90 "
              + "WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE number = 301)");

      conn.commit();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to insert initial rooms", e);
    }
  }
}
