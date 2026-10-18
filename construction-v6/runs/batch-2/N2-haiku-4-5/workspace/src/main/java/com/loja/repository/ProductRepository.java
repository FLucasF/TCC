package com.loja.repository;

import com.loja.model.Product;
import java.sql.*;

public class ProductRepository {
    private final Database db;

    public ProductRepository(Database db) {
        this.db = db;
    }

    public Product findBySku(String sku) {
        String sql = "SELECT sku, name, price, weight_kg, stock, discontinued FROM products WHERE sku = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sku);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Product(
                        rs.getString("sku"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getDouble("weight_kg"),
                        rs.getInt("stock"),
                        rs.getBoolean("discontinued")
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding product", e);
        }
        return null;
    }

    public void updateStock(String sku, int quantity) {
        String sql = "UPDATE products SET stock = stock - ? WHERE sku = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quantity);
            stmt.setString(2, sku);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating stock", e);
        }
    }

    public void restoreStock(String sku, int quantity) {
        String sql = "UPDATE products SET stock = stock + ? WHERE sku = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quantity);
            stmt.setString(2, sku);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error restoring stock", e);
        }
    }
}
