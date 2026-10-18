package com.loja.repositories.impl;

import com.loja.domain.entities.Money;
import com.loja.domain.entities.Product;
import com.loja.infrastructure.Database;
import com.loja.repositories.ProductRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ProductRepositoryImpl implements ProductRepository {
    private final Database database;

    public ProductRepositoryImpl(Database database) {
        this.database = database;
    }

    @Override
    public Product findBySku(String sku) {
        String sql = "SELECT * FROM products WHERE sku = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sku);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Product(
                        rs.getString("sku"),
                        rs.getString("name"),
                        Money.of(rs.getDouble("price")),
                        rs.getDouble("weight_kg"),
                        rs.getInt("stock"),
                        rs.getBoolean("discontinued")
                );
            }
            return null;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find product: " + sku, e);
        }
    }

    @Override
    public void update(Product product) {
        String sql = "UPDATE products SET stock = ? WHERE sku = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, product.getStock());
            stmt.setString(2, product.getSku());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update product: " + product.getSku(), e);
        }
    }

    @Override
    public void initializeCatalog() {
        String checkSql = "SELECT COUNT(*) FROM products";
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(checkSql)) {

            ResultSet rs = stmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to check catalog", e);
        }

        String insertSql = "INSERT INTO products (sku, name, price, weight_kg, stock, discontinued) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql)) {

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

            for (Object[] row : catalog) {
                stmt.setString(1, (String) row[0]);
                stmt.setString(2, (String) row[1]);
                stmt.setDouble(3, (Double) row[2]);
                stmt.setDouble(4, (Double) row[3]);
                stmt.setInt(5, (Integer) row[4]);
                stmt.setBoolean(6, (Boolean) row[5]);
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize catalog", e);
        }
    }
}
