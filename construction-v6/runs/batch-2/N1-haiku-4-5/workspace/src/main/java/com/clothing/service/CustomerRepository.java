package com.clothing.service;

import com.clothing.Database;
import com.clothing.model.Customer;
import com.clothing.model.Notification;
import com.clothing.model.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

public class CustomerRepository {
    private static final Logger logger = LoggerFactory.getLogger(CustomerRepository.class);

    public Customer findOrCreate(String cpf, String email) {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement(
                "SELECT cpf, email, credit_balance FROM customers WHERE cpf = ?"
            );
            stmt.setString(1, cpf);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                return new Customer(
                    rs.getString("cpf"),
                    rs.getString("email"),
                    new BigDecimal(rs.getString("credit_balance"))
                );
            }

            var insertStmt = conn.prepareStatement(
                "INSERT INTO customers (cpf, email, credit_balance) VALUES (?, ?, 0)"
            );
            insertStmt.setString(1, cpf);
            insertStmt.setString(2, email);
            insertStmt.executeUpdate();

            return new Customer(cpf, email, BigDecimal.ZERO);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Customer getCustomer(String cpf) {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement(
                "SELECT cpf, email, credit_balance FROM customers WHERE cpf = ?"
            );
            stmt.setString(1, cpf);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                return new Customer(
                    rs.getString("cpf"),
                    rs.getString("email"),
                    new BigDecimal(rs.getString("credit_balance"))
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public void updateCreditBalance(String cpf, BigDecimal creditDelta) {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement(
                "UPDATE customers SET credit_balance = credit_balance + ? WHERE cpf = ?"
            );
            stmt.setBigDecimal(1, creditDelta);
            stmt.setString(2, cpf);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Long> findCompletedOrders(String cpf) {
        List<Long> orders = new ArrayList<>();
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement(
                "SELECT id FROM orders WHERE customer_cpf = ? AND status IN ('PAID', 'SHIPPED', 'DELIVERED')"
            );
            stmt.setString(1, cpf);
            var rs = stmt.executeQuery();
            while (rs.next()) {
                orders.add(rs.getLong("id"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return orders;
    }

    public List<Notification> getNotifications(String cpf) {
        List<Notification> notifications = new ArrayList<>();
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement(
                "SELECT id, type, order_id, status, created_at FROM notifications WHERE customer_cpf = ? ORDER BY created_at ASC"
            );
            stmt.setString(1, cpf);
            var rs = stmt.executeQuery();
            while (rs.next()) {
                notifications.add(new Notification(
                    rs.getLong("id"),
                    NotificationType.fromString(rs.getString("type")),
                    rs.getLong("order_id"),
                    rs.getString("status"),
                    rs.getTimestamp("created_at").toInstant()
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return notifications;
    }

    public Notification getNotification(long notificationId) {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement(
                "SELECT id, type, order_id, status, created_at, customer_cpf FROM notifications WHERE id = ?"
            );
            stmt.setLong(1, notificationId);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                return new Notification(
                    rs.getLong("id"),
                    NotificationType.fromString(rs.getString("type")),
                    rs.getLong("order_id"),
                    rs.getString("status"),
                    rs.getTimestamp("created_at").toInstant()
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public void markNotificationAsRead(long notificationId) {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement(
                "UPDATE notifications SET status = 'READ' WHERE id = ?"
            );
            stmt.setLong(1, notificationId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public long createNotification(String cpf, long orderId, NotificationType type) {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement(
                "INSERT INTO notifications (customer_cpf, order_id, type, status) VALUES (?, ?, ?, 'NEW')",
                java.sql.Statement.RETURN_GENERATED_KEYS
            );
            stmt.setString(1, cpf);
            stmt.setLong(2, orderId);
            stmt.setString(3, type.toString());
            stmt.executeUpdate();

            var rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return -1;
    }
}
