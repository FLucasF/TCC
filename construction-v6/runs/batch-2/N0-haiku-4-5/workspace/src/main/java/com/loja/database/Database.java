package com.loja.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.loja.models.*;

import java.sql.*;
import java.util.*;

public class Database {
    private static HikariDataSource dataSource;

    public static void init(String dbUrl, String dbUser, String dbPassword) throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(10);
        dataSource = new HikariDataSource(config);

        migrate();
    }

    private static void migrate() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            Statement stmt = conn.createStatement();

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS products (
                    sku VARCHAR(20) PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    price DECIMAL(10, 2) NOT NULL,
                    weight_kg DECIMAL(5, 2) NOT NULL,
                    stock INT NOT NULL,
                    discontinued BOOLEAN NOT NULL DEFAULT FALSE
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS customers (
                    cpf VARCHAR(11) PRIMARY KEY,
                    email VARCHAR(255) NOT NULL,
                    credit_balance DECIMAL(10, 2) NOT NULL DEFAULT 0.00
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS orders (
                    id VARCHAR(36) PRIMARY KEY,
                    cpf VARCHAR(11) NOT NULL REFERENCES customers(cpf),
                    email VARCHAR(255) NOT NULL,
                    status VARCHAR(30) NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    products_subtotal DECIMAL(10, 2) NOT NULL,
                    coupon_discount DECIMAL(10, 2) NOT NULL,
                    shipping_cost DECIMAL(10, 2) NOT NULL,
                    delivery_days INT NOT NULL,
                    insurance DECIMAL(10, 2) NOT NULL,
                    payment_adjustment DECIMAL(10, 2) NOT NULL,
                    final_total DECIMAL(10, 2) NOT NULL,
                    installments INT NOT NULL,
                    installment_amount DECIMAL(10, 2) NOT NULL,
                    next_purchase_credit DECIMAL(10, 2) NOT NULL,
                    gift BOOLEAN NOT NULL,
                    tracking_code VARCHAR(50),
                    region VARCHAR(20) NOT NULL,
                    shipping_method VARCHAR(20) NOT NULL,
                    payment_method VARCHAR(20) NOT NULL,
                    club_tier VARCHAR(10) NOT NULL,
                    coupon VARCHAR(50),
                    paid_at TIMESTAMP,
                    payment_transaction_id VARCHAR(100)
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS order_items (
                    id SERIAL PRIMARY KEY,
                    order_id VARCHAR(36) NOT NULL REFERENCES orders(id),
                    sku VARCHAR(20) NOT NULL REFERENCES products(sku),
                    quantity INT NOT NULL
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS notifications (
                    id VARCHAR(36) PRIMARY KEY,
                    cpf VARCHAR(11) NOT NULL REFERENCES customers(cpf),
                    type VARCHAR(30) NOT NULL,
                    order_id VARCHAR(36),
                    status VARCHAR(10) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);

            stmt.close();
        }

        loadInitialCatalog();
    }

    private static void loadInitialCatalog() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM products");
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            if (rs.getInt(1) == 0) {
                insertProduct(conn, "CAM-001", "Camiseta básica", 79.90, 0.30, 50, false);
                insertProduct(conn, "TEN-001", "Tênis corrida", 249.90, 1.20, 10, false);
                insertProduct(conn, "FON-001", "Fone sem fio", 199.90, 0.25, 5, false);
                insertProduct(conn, "MEI-001", "Meia esportiva", 19.90, 0.10, 100, false);
                insertProduct(conn, "JAQ-001", "Jaqueta corta-vento", 389.90, 0.80, 3, false);
                insertProduct(conn, "MOC-001", "Mochila urbana", 159.90, 0.90, 1, false);
                insertProduct(conn, "BON-001", "Boné aba reta", 59.90, 0.15, 0, false);
                insertProduct(conn, "REL-001", "Relógio digital", 129.90, 0.20, 8, true);
                conn.commit();
            }
            rs.close();
            pstmt.close();
        }
    }

    private static void insertProduct(Connection conn, String sku, String name, double price,
                                     double weightKg, int stock, boolean discontinued) throws SQLException {
        String sql = "INSERT INTO products (sku, name, price, weight_kg, stock, discontinued) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, sku);
        pstmt.setString(2, name);
        pstmt.setDouble(3, price);
        pstmt.setDouble(4, weightKg);
        pstmt.setInt(5, stock);
        pstmt.setBoolean(6, discontinued);
        pstmt.executeUpdate();
        pstmt.close();
    }

    public static HikariDataSource getDataSource() {
        return dataSource;
    }

    public static void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
