package com.loja.repositories.impl;

import com.loja.domain.entities.Customer;
import com.loja.domain.entities.Money;
import com.loja.infrastructure.Database;
import com.loja.repositories.CustomerRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CustomerRepositoryImpl implements CustomerRepository {
    private final Database database;

    public CustomerRepositoryImpl(Database database) {
        this.database = database;
    }

    @Override
    public Customer findByCpf(String cpf) {
        String sql = "SELECT * FROM customers WHERE cpf = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cpf);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Customer(
                        rs.getString("cpf"),
                        Money.of(rs.getDouble("credit_balance"))
                );
            }
            return null;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find customer: " + cpf, e);
        }
    }

    @Override
    public void save(Customer customer) {
        String sql = "INSERT INTO customers (cpf, credit_balance) VALUES (?, ?)";
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, customer.getCpf());
            stmt.setDouble(2, customer.getCreditBalance().toDouble());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save customer: " + customer.getCpf(), e);
        }
    }

    @Override
    public void update(Customer customer) {
        String sql = "UPDATE customers SET credit_balance = ? WHERE cpf = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, customer.getCreditBalance().toDouble());
            stmt.setString(2, customer.getCpf());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update customer: " + customer.getCpf(), e);
        }
    }

    @Override
    public boolean exists(String cpf) {
        return findByCpf(cpf) != null;
    }
}
