package com.clothing.service;

import com.clothing.Database;
import com.clothing.model.Product;
import java.sql.Connection;
import java.sql.SQLException;

public class ProductRepository {
    public Product findBySku(String sku) {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement(
                "SELECT sku, name, price, weight_kg, stock, discontinued FROM products WHERE sku = ?"
            );
            stmt.setString(1, sku);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                return new Product(
                    rs.getString("sku"),
                    rs.getString("name"),
                    new java.math.BigDecimal(rs.getString("price")),
                    new java.math.BigDecimal(rs.getString("weight_kg")),
                    rs.getInt("stock"),
                    rs.getBoolean("discontinued")
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
