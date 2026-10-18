package com.loja.repository;

import com.loja.domain.Customer;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Optional;

public class CustomerRepository {
    private final Database database;

    public CustomerRepository(Database database) {
        this.database = database;
    }

    public Optional<Customer> findByCpf(String cpf) {
        String sql = "SELECT * FROM customers WHERE cpf = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cpf);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapCustomer(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find customer", e);
        }
        return Optional.empty();
    }

    public void create(String cpf, String email, Connection conn) throws SQLException {
        String sql = "INSERT INTO customers (cpf, email, credit_balance) VALUES (?, ?, 0) ON CONFLICT (cpf) DO NOTHING";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cpf);
            pstmt.setString(2, email);
            pstmt.executeUpdate();
        }
    }

    public void addCredit(String cpf, BigDecimal amount, Connection conn) throws SQLException {
        String sql = "UPDATE customers SET credit_balance = credit_balance + ? WHERE cpf = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBigDecimal(1, amount);
            pstmt.setString(2, cpf);
            pstmt.executeUpdate();
        }
    }

    public void subtractCredit(String cpf, BigDecimal amount, Connection conn) throws SQLException {
        String sql = "UPDATE customers SET credit_balance = credit_balance - ? WHERE cpf = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBigDecimal(1, amount);
            pstmt.setString(2, cpf);
            pstmt.executeUpdate();
        }
    }

    private Customer mapCustomer(ResultSet rs) throws SQLException {
        return new Customer(
            rs.getString("cpf"),
            rs.getString("email"),
            rs.getBigDecimal("credit_balance")
        );
    }
}
