package com.loja.repositories.impl;

import com.loja.domain.entities.*;
import com.loja.infrastructure.Database;
import com.loja.repositories.OrderRepository;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class OrderRepositoryImpl implements OrderRepository {
    private final Database database;

    public OrderRepositoryImpl(Database database) {
        this.database = database;
    }

    @Override
    public void save(Order order) {
        String orderSql = """
            INSERT INTO orders (
                id, customer_cpf, customer_email, status, created_at,
                products_subtotal, coupon_discount, shipping_cost, delivery_days, insurance,
                payment_adjustment, final_total, installments, installment_amount,
                next_purchase_credit, gift
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        String itemSql = "INSERT INTO order_items (order_id, sku, quantity) VALUES (?, ?, ?)";

        try (Connection conn = database.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement orderStmt = conn.prepareStatement(orderSql);
                 PreparedStatement itemStmt = conn.prepareStatement(itemSql)) {

                OrderSummary summary = order.getSummary();

                orderStmt.setString(1, order.getId());
                orderStmt.setString(2, order.getCustomerCpf());
                orderStmt.setString(3, order.getCustomerEmail());
                orderStmt.setString(4, order.getStatus().name());
                orderStmt.setTimestamp(5, Timestamp.from(order.getCreatedAt()));
                orderStmt.setDouble(6, summary.getProductsSubtotal().toDouble());
                orderStmt.setDouble(7, summary.getCouponDiscount().toDouble());
                orderStmt.setDouble(8, summary.getShippingCost().toDouble());
                orderStmt.setInt(9, summary.getDeliveryDays());
                orderStmt.setDouble(10, summary.getInsurance().toDouble());
                orderStmt.setDouble(11, summary.getPaymentAdjustment().toDouble());
                orderStmt.setDouble(12, summary.getFinalTotal().toDouble());
                orderStmt.setInt(13, summary.getInstallments());
                orderStmt.setDouble(14, summary.getInstallmentAmount().toDouble());
                orderStmt.setDouble(15, summary.getNextPurchaseCredit().toDouble());
                orderStmt.setBoolean(16, summary.isGift());
                orderStmt.executeUpdate();

                for (OrderItem item : order.getItems()) {
                    itemStmt.setString(1, order.getId());
                    itemStmt.setString(2, item.getSku());
                    itemStmt.setInt(3, item.getQuantity());
                    itemStmt.executeUpdate();
                }

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save order: " + order.getId(), e);
        }
    }

    @Override
    public Order findById(String id) {
        String orderSql = "SELECT * FROM orders WHERE id = ?";
        String itemsSql = "SELECT * FROM order_items WHERE order_id = ?";

        try (Connection conn = database.getConnection();
             PreparedStatement orderStmt = conn.prepareStatement(orderSql);
             PreparedStatement itemsStmt = conn.prepareStatement(itemsSql)) {

            orderStmt.setString(1, id);
            ResultSet orderRs = orderStmt.executeQuery();

            if (!orderRs.next()) {
                return null;
            }

            itemsStmt.setString(1, id);
            ResultSet itemsRs = itemsStmt.executeQuery();

            List<OrderItem> items = new ArrayList<>();
            while (itemsRs.next()) {
                items.add(new OrderItem(
                        itemsRs.getString("sku"),
                        itemsRs.getInt("quantity")
                ));
            }

            OrderSummary summary = new OrderSummary(
                    Money.of(orderRs.getDouble("products_subtotal")),
                    Money.of(orderRs.getDouble("coupon_discount")),
                    Money.of(orderRs.getDouble("shipping_cost")),
                    orderRs.getInt("delivery_days"),
                    Money.of(orderRs.getDouble("insurance")),
                    Money.of(orderRs.getDouble("payment_adjustment")),
                    Money.of(orderRs.getDouble("final_total")),
                    orderRs.getInt("installments"),
                    Money.of(orderRs.getDouble("installment_amount")),
                    Money.of(orderRs.getDouble("next_purchase_credit")),
                    orderRs.getBoolean("gift")
            );

            Timestamp paidAtTs = orderRs.getTimestamp("paid_at");
            Instant paidAt = paidAtTs != null ? paidAtTs.toInstant() : null;

            return new Order(
                    orderRs.getString("id"),
                    orderRs.getString("customer_cpf"),
                    orderRs.getString("customer_email"),
                    items,
                    summary,
                    OrderStatus.valueOf(orderRs.getString("status")),
                    orderRs.getTimestamp("created_at").toInstant(),
                    orderRs.getString("tracking_code"),
                    orderRs.getString("payment_transaction_id"),
                    paidAt
            );

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find order: " + id, e);
        }
    }

    @Override
    public void update(Order order) {
        String sql = """
            UPDATE orders SET
                status = ?,
                tracking_code = ?,
                payment_transaction_id = ?,
                paid_at = ?
            WHERE id = ?
        """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, order.getStatus().name());
            stmt.setString(2, order.getTrackingCode());
            stmt.setString(3, order.getPaymentTransactionId());

            if (order.getPaidAt() != null) {
                stmt.setTimestamp(4, Timestamp.from(order.getPaidAt()));
            } else {
                stmt.setNull(4, Types.TIMESTAMP);
            }

            stmt.setString(5, order.getId());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update order: " + order.getId(), e);
        }
    }

    @Override
    public boolean isFirstOrder(String customerCpf) {
        String sql = """
            SELECT COUNT(*) FROM orders
            WHERE customer_cpf = ?
            AND status IN ('PAID', 'SHIPPED', 'DELIVERED')
        """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, customerCpf);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) == 0;
            }
            return true;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to check first order for customer: " + customerCpf, e);
        }
    }

    @Override
    public boolean hasPaymentTransaction(String orderId, String transactionId) {
        String sql = "SELECT COUNT(*) FROM payment_transactions WHERE order_id = ? AND transaction_id = ?";

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, orderId);
            stmt.setString(2, transactionId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to check payment transaction", e);
        }
    }

    public void savePaymentTransaction(String orderId, String transactionId) {
        String sql = "INSERT INTO payment_transactions (order_id, transaction_id) VALUES (?, ?)";

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, orderId);
            stmt.setString(2, transactionId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save payment transaction", e);
        }
    }
}
