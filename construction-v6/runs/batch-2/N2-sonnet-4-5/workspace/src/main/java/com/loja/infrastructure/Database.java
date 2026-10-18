package com.loja.infrastructure;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private final String url;
    private final String user;
    private final String password;

    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public void initialize() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS products (
                    sku VARCHAR(50) PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    price DECIMAL(10, 2) NOT NULL,
                    weight_kg DECIMAL(10, 3) NOT NULL,
                    stock INTEGER NOT NULL,
                    discontinued BOOLEAN NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS customers (
                    cpf VARCHAR(11) PRIMARY KEY,
                    credit_balance DECIMAL(10, 2) NOT NULL DEFAULT 0
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS orders (
                    id VARCHAR(50) PRIMARY KEY,
                    customer_cpf VARCHAR(11) NOT NULL,
                    customer_email VARCHAR(255) NOT NULL,
                    status VARCHAR(50) NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    tracking_code VARCHAR(255),
                    payment_transaction_id VARCHAR(255),
                    paid_at TIMESTAMP,
                    products_subtotal DECIMAL(10, 2) NOT NULL,
                    coupon_discount DECIMAL(10, 2) NOT NULL,
                    shipping_cost DECIMAL(10, 2) NOT NULL,
                    delivery_days INTEGER NOT NULL,
                    insurance DECIMAL(10, 2) NOT NULL,
                    payment_adjustment DECIMAL(10, 2) NOT NULL,
                    final_total DECIMAL(10, 2) NOT NULL,
                    installments INTEGER NOT NULL,
                    installment_amount DECIMAL(10, 2) NOT NULL,
                    next_purchase_credit DECIMAL(10, 2) NOT NULL,
                    gift BOOLEAN NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS order_items (
                    order_id VARCHAR(50) NOT NULL,
                    sku VARCHAR(50) NOT NULL,
                    quantity INTEGER NOT NULL,
                    PRIMARY KEY (order_id, sku),
                    FOREIGN KEY (order_id) REFERENCES orders(id)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS notifications (
                    id SERIAL PRIMARY KEY,
                    customer_cpf VARCHAR(11) NOT NULL,
                    type VARCHAR(50) NOT NULL,
                    order_id VARCHAR(50) NOT NULL,
                    status VARCHAR(50) NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS payment_transactions (
                    order_id VARCHAR(50) NOT NULL,
                    transaction_id VARCHAR(255) NOT NULL,
                    PRIMARY KEY (order_id, transaction_id)
                )
            """);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
}
