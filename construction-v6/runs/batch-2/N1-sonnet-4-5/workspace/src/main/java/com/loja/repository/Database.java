package com.loja.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private final DataSource dataSource;

    public Database(String dbUrl, String dbUser, String dbPassword) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(10);
        this.dataSource = new HikariDataSource(config);
        initializeSchema();
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private void initializeSchema() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS products (
                    sku VARCHAR(50) PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    price DECIMAL(10, 2) NOT NULL,
                    weight_kg DECIMAL(10, 3) NOT NULL,
                    stock INTEGER NOT NULL,
                    discontinued BOOLEAN NOT NULL DEFAULT FALSE
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS customers (
                    cpf VARCHAR(11) PRIMARY KEY,
                    email VARCHAR(255) NOT NULL,
                    credit_balance DECIMAL(10, 2) NOT NULL DEFAULT 0
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS orders (
                    id VARCHAR(50) PRIMARY KEY,
                    customer_cpf VARCHAR(11) NOT NULL,
                    customer_email VARCHAR(255) NOT NULL,
                    zip_code VARCHAR(8) NOT NULL,
                    region VARCHAR(20) NOT NULL,
                    shipping_method VARCHAR(20) NOT NULL,
                    coupon VARCHAR(50),
                    payment_method VARCHAR(20) NOT NULL,
                    club_tier VARCHAR(20) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    tracking_code VARCHAR(100),
                    payment_transaction_id VARCHAR(100),
                    summary_json TEXT NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS order_items (
                    order_id VARCHAR(50) NOT NULL,
                    sku VARCHAR(50) NOT NULL,
                    quantity INTEGER NOT NULL,
                    PRIMARY KEY (order_id, sku)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS notifications (
                    id VARCHAR(50) PRIMARY KEY,
                    customer_cpf VARCHAR(11) NOT NULL,
                    type VARCHAR(30) NOT NULL,
                    order_id VARCHAR(50) NOT NULL,
                    status VARCHAR(10) NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS payment_transactions (
                    order_id VARCHAR(50) NOT NULL,
                    transaction_id VARCHAR(100) NOT NULL,
                    paid_at TIMESTAMP NOT NULL,
                    PRIMARY KEY (order_id, transaction_id)
                )
            """);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }
}
