package com.loja.services;

import com.loja.database.Database;
import com.loja.models.Product;

import java.sql.*;

public class ProductService {
    public static Product getProduct(String sku) throws SQLException {
        String sql = "SELECT sku, name, price, weight_kg, stock, discontinued FROM products WHERE sku = ?";
        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sku);
            ResultSet rs = pstmt.executeQuery();
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
            return null;
        }
    }

    public static void updateStock(String sku, int quantity) throws SQLException {
        String sql = "UPDATE products SET stock = stock + ? WHERE sku = ?";
        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, quantity);
            pstmt.setString(2, sku);
            pstmt.executeUpdate();
        }
    }
}
