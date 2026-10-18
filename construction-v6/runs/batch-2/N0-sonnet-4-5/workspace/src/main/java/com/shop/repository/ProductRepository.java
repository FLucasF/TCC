package com.shop.repository;

import com.shop.config.Database;
import com.shop.model.Product;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class ProductRepository {
    private final Database database;

    public ProductRepository(Database database) {
        this.database = database;
    }

    public Product findBySku(String sku) {
        String sql = "SELECT * FROM products WHERE sku = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sku);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapProduct(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find product", e);
        }
    }

    public Map<String, Product> findBySkus(Iterable<String> skus) {
        Map<String, Product> result = new HashMap<>();
        for (String sku : skus) {
            Product product = findBySku(sku);
            if (product != null) {
                result.put(sku, product);
            }
        }
        return result;
    }

    public void decrementStock(Connection conn, String sku, int quantity) throws SQLException {
        String sql = "UPDATE products SET stock = stock - ? WHERE sku = ? AND stock >= ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quantity);
            stmt.setString(2, sku);
            stmt.setInt(3, quantity);
            int updated = stmt.executeUpdate();
            if (updated == 0) {
                throw new SQLException("Insufficient stock for " + sku);
            }
        }
    }

    public void incrementStock(Connection conn, String sku, int quantity) throws SQLException {
        String sql = "UPDATE products SET stock = stock + ? WHERE sku = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quantity);
            stmt.setString(2, sku);
            stmt.executeUpdate();
        }
    }

    private Product mapProduct(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setSku(rs.getString("sku"));
        product.setName(rs.getString("name"));
        product.setPrice(rs.getBigDecimal("price"));
        product.setWeightKg(rs.getBigDecimal("weight_kg"));
        product.setStock(rs.getInt("stock"));
        product.setDiscontinued(rs.getBoolean("discontinued"));
        return product;
    }
}
