package com.pousada.marealta.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
  private static final Logger logger = LoggerFactory.getLogger(Database.class);
  private static HikariDataSource dataSource;

  public static void init(String dbUrl, String dbUser, String dbPassword) {
    if (dataSource != null) {
      return;
    }

    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(dbUrl);
    config.setUsername(dbUser);
    config.setPassword(dbPassword);
    config.setMaximumPoolSize(10);
    config.setMinimumIdle(2);
    config.setConnectionTimeout(30000);
    config.setIdleTimeout(600000);
    config.setMaxLifetime(1800000);

    dataSource = new HikariDataSource(config);
    logger.info("Database connection pool initialized");

    runMigrations();
  }

  private static void runMigrations() {
    try (Connection conn = dataSource.getConnection()) {
      Migration.migrate(conn);
      logger.info("Database migrations completed");
    } catch (SQLException e) {
      logger.error("Error running migrations", e);
      throw new RuntimeException("Failed to initialize database", e);
    }
  }

  public static Connection getConnection() throws SQLException {
    if (dataSource == null) {
      throw new IllegalStateException("Database not initialized");
    }
    return dataSource.getConnection();
  }

  public static DataSource getDataSource() {
    return dataSource;
  }

  public static void close() {
    if (dataSource != null) {
      dataSource.close();
      dataSource = null;
    }
  }
}
