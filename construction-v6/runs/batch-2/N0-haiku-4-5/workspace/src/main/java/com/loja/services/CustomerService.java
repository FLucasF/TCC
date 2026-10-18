package com.loja.services;

import com.loja.database.Database;
import com.loja.models.Customer;

import java.sql.*;

public class CustomerService {
    public static Customer getOrCreateCustomer(String cpf, String email) throws SQLException {
        String selectSql = "SELECT cpf, email, credit_balance FROM customers WHERE cpf = ?";
        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
            pstmt.setString(1, cpf);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Customer(rs.getString("cpf"), rs.getDouble("credit_balance"));
            }
        }

        String insertSql = "INSERT INTO customers (cpf, email, credit_balance) VALUES (?, ?, 0.00)";
        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            pstmt.setString(1, cpf);
            pstmt.setString(2, email);
            pstmt.executeUpdate();
        }
        return new Customer(cpf, 0.0);
    }

    public static Customer getCustomer(String cpf) throws SQLException {
        String sql = "SELECT cpf, credit_balance FROM customers WHERE cpf = ?";
        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cpf);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Customer(rs.getString("cpf"), rs.getDouble("credit_balance"));
            }
            return null;
        }
    }

    public static void updateCreditBalance(String cpf, double amount) throws SQLException {
        String sql = "UPDATE customers SET credit_balance = credit_balance + ? WHERE cpf = ?";
        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, amount);
            pstmt.setString(2, cpf);
            pstmt.executeUpdate();
        }
    }
}
