package com.loja.repository;

import com.loja.model.Customer;
import java.sql.*;

public class CustomerRepository {
    private final Database db;

    public CustomerRepository(Database db) {
        this.db = db;
    }

    public Customer findByCpf(String cpf) {
        String sql = "SELECT cpf, credit_balance FROM customers WHERE cpf = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cpf);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Customer(
                        rs.getString("cpf"),
                        rs.getDouble("credit_balance")
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding customer", e);
        }
        return null;
    }

    public void createOrUpdate(String cpf, Double creditBalance) {
        String sql = "INSERT INTO customers (cpf, credit_balance) VALUES (?, ?) " +
                     "ON CONFLICT (cpf) DO UPDATE SET credit_balance = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cpf);
            stmt.setDouble(2, creditBalance);
            stmt.setDouble(3, creditBalance);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error creating/updating customer", e);
        }
    }

    public void addCredit(String cpf, Double amount) {
        String sql = "UPDATE customers SET credit_balance = credit_balance + ? WHERE cpf = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, amount);
            stmt.setString(2, cpf);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error adding credit", e);
        }
    }

    public void subtractCredit(String cpf, Double amount) {
        String sql = "UPDATE customers SET credit_balance = credit_balance - ? WHERE cpf = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, amount);
            stmt.setString(2, cpf);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error subtracting credit", e);
        }
    }
}
