package com.shop.repository;

import com.shop.config.Database;
import com.shop.model.Customer;

import java.math.BigDecimal;
import java.sql.*;

public class CustomerRepository {
    private final Database database;

    public CustomerRepository(Database database) {
        this.database = database;
    }

    public Customer findByCpf(String cpf) {
        String sql = "SELECT * FROM customers WHERE cpf = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cpf);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapCustomer(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find customer", e);
        }
    }

    public Customer findByCpfForUpdate(Connection conn, String cpf) throws SQLException {
        String sql = "SELECT * FROM customers WHERE cpf = ? FOR UPDATE";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cpf);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapCustomer(rs);
                }
                return null;
            }
        }
    }

    public void create(Connection conn, String cpf, String email) throws SQLException {
        String sql = "INSERT INTO customers (cpf, email, credit_balance) VALUES (?, ?, 0) ON CONFLICT (cpf) DO NOTHING";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cpf);
            stmt.setString(2, email);
            stmt.executeUpdate();
        }
    }

    public void addCredit(Connection conn, String cpf, BigDecimal amount) throws SQLException {
        String sql = "UPDATE customers SET credit_balance = credit_balance + ? WHERE cpf = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, amount);
            stmt.setString(2, cpf);
            stmt.executeUpdate();
        }
    }

    public void subtractCredit(Connection conn, String cpf, BigDecimal amount) throws SQLException {
        String sql = "UPDATE customers SET credit_balance = credit_balance - ? WHERE cpf = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, amount);
            stmt.setString(2, cpf);
            stmt.executeUpdate();
        }
    }

    private Customer mapCustomer(ResultSet rs) throws SQLException {
        Customer customer = new Customer();
        customer.setCpf(rs.getString("cpf"));
        customer.setEmail(rs.getString("email"));
        customer.setCreditBalance(rs.getBigDecimal("credit_balance"));
        return customer;
    }
}
