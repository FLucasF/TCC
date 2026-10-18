package com.clothing;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class Database {
    private static final Logger logger = LoggerFactory.getLogger(Database.class);
    private static HikariDataSource dataSource;

    public static void initialize() {
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");

        if (url == null || user == null || password == null) {
            throw new IllegalStateException("DB_URL, DB_USER and DB_PASSWORD environment variables must be set");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setAutoCommit(true);

        dataSource = new HikariDataSource(config);
        logger.info("Database connection pool created");

        createSchema();
    }

    private static void createSchema() {
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS products (" +
                "sku VARCHAR(20) PRIMARY KEY, " +
                "name VARCHAR(255) NOT NULL, " +
                "price NUMERIC(10,2) NOT NULL, " +
                "weight_kg NUMERIC(5,2) NOT NULL, " +
                "stock INT NOT NULL, " +
                "discontinued BOOLEAN NOT NULL DEFAULT FALSE)"
            );

            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS customers (" +
                "cpf VARCHAR(11) PRIMARY KEY, " +
                "email VARCHAR(255) NOT NULL, " +
                "credit_balance NUMERIC(10,2) NOT NULL DEFAULT 0, " +
                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)"
            );

            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS orders (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "customer_cpf VARCHAR(11) NOT NULL, " +
                "status VARCHAR(20) NOT NULL, " +
                "products_subtotal NUMERIC(10,2) NOT NULL, " +
                "coupon_discount NUMERIC(10,2) NOT NULL, " +
                "shipping_cost NUMERIC(10,2) NOT NULL, " +
                "delivery_days INT NOT NULL, " +
                "insurance NUMERIC(10,2) NOT NULL, " +
                "payment_adjustment NUMERIC(10,2) NOT NULL, " +
                "final_total NUMERIC(10,2) NOT NULL, " +
                "club_tier VARCHAR(10) NOT NULL, " +
                "region VARCHAR(20) NOT NULL, " +
                "shipping_method VARCHAR(20) NOT NULL, " +
                "payment_method VARCHAR(20) NOT NULL, " +
                "installments INT NOT NULL, " +
                "next_purchase_credit NUMERIC(10,2) NOT NULL, " +
                "gift BOOLEAN NOT NULL, " +
                "tracking_code VARCHAR(50), " +
                "created_at TIMESTAMP NOT NULL, " +
                "paid_at TIMESTAMP, " +
                "shipped_at TIMESTAMP, " +
                "delivered_at TIMESTAMP)"
            );

            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS order_items (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "order_id BIGINT NOT NULL, " +
                "sku VARCHAR(20) NOT NULL, " +
                "quantity INT NOT NULL, " +
                "FOREIGN KEY (order_id) REFERENCES orders(id))"
            );

            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS notifications (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "customer_cpf VARCHAR(11) NOT NULL, " +
                "order_id BIGINT NOT NULL, " +
                "type VARCHAR(30) NOT NULL, " +
                "status VARCHAR(10) NOT NULL, " +
                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (customer_cpf) REFERENCES customers(cpf), " +
                "FOREIGN KEY (order_id) REFERENCES orders(id))"
            );

            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS payment_transactions (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "order_id BIGINT NOT NULL UNIQUE, " +
                "transaction_id VARCHAR(100) NOT NULL UNIQUE, " +
                "paid_at TIMESTAMP NOT NULL, " +
                "FOREIGN KEY (order_id) REFERENCES orders(id))"
            );

            conn.createStatement().execute(
                "CREATE INDEX IF NOT EXISTS idx_orders_cpf ON orders(customer_cpf)"
            );

            conn.createStatement().execute(
                "CREATE INDEX IF NOT EXISTS idx_notifications_cpf ON notifications(customer_cpf)"
            );

            logger.info("Database schema created");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create schema", e);
        }

        seedInitialProducts();
    }

    private static void seedInitialProducts() {
        try (Connection conn = dataSource.getConnection()) {
            var rs = conn.createStatement().executeQuery("SELECT COUNT(*) as count FROM products");
            rs.next();
            if (rs.getInt("count") == 0) {
                conn.createStatement().execute(
                    "INSERT INTO products (sku, name, price, weight_kg, stock, discontinued) VALUES " +
                    "('CAM-001', 'Camiseta básica', 79.90, 0.30, 50, false), " +
                    "('TEN-001', 'Tênis corrida', 249.90, 1.20, 10, false), " +
                    "('FON-001', 'Fone sem fio', 199.90, 0.25, 5, false), " +
                    "('MEI-001', 'Meia esportiva', 19.90, 0.10, 100, false), " +
                    "('JAQ-001', 'Jaqueta corta-vento', 389.90, 0.80, 3, false), " +
                    "('MOC-001', 'Mochila urbana', 159.90, 0.90, 1, false), " +
                    "('BON-001', 'Boné aba reta', 59.90, 0.15, 0, false), " +
                    "('REL-001', 'Relógio digital', 129.90, 0.20, 8, true)"
                );
                logger.info("Initial products seeded");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to seed products", e);
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
