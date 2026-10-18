package com.loja.repository;

import com.loja.domain.Product;

import java.sql.*;
import java.util.*;

public class ProductRepository {
    private final Database database;

    public ProductRepository(Database database) {
        this.database = database;
    }

    public void initializeCatalog() {
        try (Connection conn = database.getConnection()) {
            String checkSql = "SELECT COUNT(*) FROM products";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(checkSql)) {
                rs.next();
                if (rs.getInt(1) > 0) {
                    return;
                }
            }

            String insertSql = "INSERT INTO products (sku, name, price, weight_kg, stock, discontinued) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                Object[][] catalog = {
                    {"CAM-001", "Camiseta básica", 79.90, 0.30, 50, false},
                    {"TEN-001", "Tênis corrida", 249.90, 1.20, 10, false},
                    {"FON-001", "Fone sem fio", 199.90, 0.25, 5, false},
                    {"MEI-001", "Meia esportiva", 19.90, 0.10, 100, false},
                    {"JAQ-001", "Jaqueta corta-vento", 389.90, 0.80, 3, false},
                    {"MOC-001", "Mochila urbana", 159.90, 0.90, 1, false},
                    {"BON-001", "Boné aba reta", 59.90, 0.15, 0, false},
                    {"REL-001", "Relógio digital", 129.90, 0.20, 8, true}
                };

                for (Object[] item : catalog) {
                    pstmt.setString(1, (String) item[0]);
                    pstmt.setString(2, (String) item[1]);
                    pstmt.setBigDecimal(3, new java.math.BigDecimal(item[2].toString()));
                    pstmt.setBigDecimal(4, new java.math.BigDecimal(item[3].toString()));
                    pstmt.setInt(5, (Integer) item[4]);
                    pstmt.setBoolean(6, (Boolean) item[5]);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize catalog", e);
        }
    }

    public Optional<Product> findBySku(String sku) {
        String sql = "SELECT * FROM products WHERE sku = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sku);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapProduct(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find product", e);
        }
        return Optional.empty();
    }

    public Map<String, Product> findBySkus(Collection<String> skus) {
        if (skus.isEmpty()) {
            return Collections.emptyMap();
        }

        String placeholders = String.join(",", Collections.nCopies(skus.size(), "?"));
        String sql = "SELECT * FROM products WHERE sku IN (" + placeholders + ")";

        Map<String, Product> products = new HashMap<>();
        try (Connection conn = database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int index = 1;
            for (String sku : skus) {
                pstmt.setString(index++, sku);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Product product = mapProduct(rs);
                    products.put(product.getSku(), product);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find products", e);
        }
        return products;
    }

    public void decreaseStock(Map<String, Integer> skuQuantities, Connection conn) throws SQLException {
        String sql = "UPDATE products SET stock = stock - ? WHERE sku = ? AND stock >= ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Map.Entry<String, Integer> entry : skuQuantities.entrySet()) {
                pstmt.setInt(1, entry.getValue());
                pstmt.setString(2, entry.getKey());
                pstmt.setInt(3, entry.getValue());
                int updated = pstmt.executeUpdate();
                if (updated == 0) {
                    throw new SQLException("INSUFFICIENT_STOCK");
                }
            }
        }
    }

    public void increaseStock(Map<String, Integer> skuQuantities, Connection conn) throws SQLException {
        String sql = "UPDATE products SET stock = stock + ? WHERE sku = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Map.Entry<String, Integer> entry : skuQuantities.entrySet()) {
                pstmt.setInt(1, entry.getValue());
                pstmt.setString(2, entry.getKey());
                pstmt.executeUpdate();
            }
        }
    }

    private Product mapProduct(ResultSet rs) throws SQLException {
        return new Product(
            rs.getString("sku"),
            rs.getString("name"),
            rs.getBigDecimal("price"),
            rs.getBigDecimal("weight_kg"),
            rs.getInt("stock"),
            rs.getBoolean("discontinued")
        );
    }
}
