package com.loja.repository;

import java.sql.*;
import java.util.Scanner;

public class Database {
    private final String url;
    private final String user;
    private final String password;

    public Database() {
        this.url = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/order_service");
        this.user = System.getenv().getOrDefault("DB_USER", "postgres");
        this.password = System.getenv().getOrDefault("DB_PASSWORD", "postgres");
    }

    public void initialize() {
        try {
            Class.forName("org.postgresql.Driver");
            executeSchema();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL driver not found", e);
        }
    }

    private void executeSchema() {
        String schema = readSchemaFile();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            for (String statement : schema.split(";")) {
                if (statement.trim().isEmpty()) continue;
                stmt.execute(statement);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize schema", e);
        }
    }

    private String readSchemaFile() {
        try (Scanner scanner = new Scanner(
            Database.class.getResourceAsStream("/schema.sql"),
            "UTF-8")) {
            return scanner.useDelimiter("\\A").next();
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
