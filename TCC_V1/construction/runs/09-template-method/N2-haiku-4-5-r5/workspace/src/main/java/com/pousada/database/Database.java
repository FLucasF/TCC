package com.pousada.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;

public class Database {
    private static HikariDataSource dataSource;

    public static void init() {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new RuntimeException("Database environment variables not set: DB_URL, DB_USER, DB_PASSWORD");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(20000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(1200000);

        dataSource = new HikariDataSource(config);

        try {
            initializeSchema();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }

    public static Connection getConnection() {
        if (dataSource == null) {
            throw new RuntimeException("Database not initialized");
        }
        try {
            return dataSource.getConnection();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get database connection", e);
        }
    }

    public static void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    private static void initializeSchema() throws Exception {
        try (InputStream is = Database.class.getResourceAsStream("/schema.sql");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is));
             Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            String line;
            StringBuilder sql = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("--")) {
                    sql.append(line).append(" ");
                    if (line.endsWith(";")) {
                        stmt.execute(sql.toString());
                        sql.setLength(0);
                    }
                }
            }
        }
    }
}
