package com.clothing.service;

import com.clothing.Database;
import com.clothing.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class OrderRepository {
    private static final Logger logger = LoggerFactory.getLogger(OrderRepository.class);

    public Order createOrder(CheckoutRequest req, Summary summary) {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement(
                "INSERT INTO orders (customer_cpf, status, products_subtotal, coupon_discount, shipping_cost, " +
                "delivery_days, insurance, payment_adjustment, final_total, club_tier, region, shipping_method, " +
                "payment_method, installments, next_purchase_credit, gift, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                java.sql.Statement.RETURN_GENERATED_KEYS
            );

            stmt.setString(1, req.customer().cpf());
            stmt.setString(2, OrderStatus.AWAITING_PAYMENT.toString());
            stmt.setBigDecimal(3, summary.productsSubtotal());
            stmt.setBigDecimal(4, summary.couponDiscount());
            stmt.setBigDecimal(5, summary.shippingCost());
            stmt.setInt(6, summary.deliveryDays());
            stmt.setBigDecimal(7, summary.insurance());
            stmt.setBigDecimal(8, summary.paymentAdjustment());
            stmt.setBigDecimal(9, summary.finalTotal());
            stmt.setString(10, req.clubTier().toString());
            stmt.setString(11, req.region().toString());
            stmt.setString(12, req.shippingMethod().toString());
            stmt.setString(13, req.paymentMethod().toString());
            stmt.setInt(14, summary.installments());
            stmt.setBigDecimal(15, summary.nextPurchaseCredit());
            stmt.setBoolean(16, summary.gift());
            stmt.setTimestamp(17, Timestamp.from(Instant.now()));

            stmt.executeUpdate();

            var rs = stmt.getGeneratedKeys();
            long orderId = -1;
            if (rs.next()) {
                orderId = rs.getLong(1);
            }

            for (OrderItem item : req.items()) {
                var itemStmt = conn.prepareStatement(
                    "INSERT INTO order_items (order_id, sku, quantity) VALUES (?, ?, ?)"
                );
                itemStmt.setLong(1, orderId);
                itemStmt.setString(2, item.sku());
                itemStmt.setInt(3, item.quantity());
                itemStmt.executeUpdate();
            }

            return getOrder(orderId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Order getOrder(long orderId) {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement(
                "SELECT id, status, products_subtotal, coupon_discount, shipping_cost, delivery_days, " +
                "insurance, payment_adjustment, final_total, installments, next_purchase_credit, gift, " +
                "tracking_code, created_at FROM orders WHERE id = ?"
            );
            stmt.setLong(1, orderId);
            var rs = stmt.executeQuery();

            if (!rs.next()) {
                return null;
            }

            var summary = new Summary(
                new BigDecimal(rs.getString("products_subtotal")),
                new BigDecimal(rs.getString("coupon_discount")),
                new BigDecimal(rs.getString("shipping_cost")),
                rs.getInt("delivery_days"),
                new BigDecimal(rs.getString("insurance")),
                new BigDecimal(rs.getString("payment_adjustment")),
                new BigDecimal(rs.getString("final_total")),
                rs.getInt("installments"),
                Money.divide(new BigDecimal(rs.getString("final_total")), new BigDecimal(rs.getInt("installments"))),
                new BigDecimal(rs.getString("next_purchase_credit")),
                rs.getBoolean("gift")
            );

            List<OrderItem> items = new ArrayList<>();
            var itemStmt = conn.prepareStatement("SELECT sku, quantity FROM order_items WHERE order_id = ?");
            itemStmt.setLong(1, orderId);
            var itemRs = itemStmt.executeQuery();
            while (itemRs.next()) {
                items.add(new OrderItem(itemRs.getString("sku"), itemRs.getInt("quantity")));
            }

            return new Order(
                orderId,
                OrderStatus.fromString(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(),
                summary,
                items,
                rs.getString("tracking_code")
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateOrderStatus(long orderId, OrderStatus status) {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement("UPDATE orders SET status = ? WHERE id = ?");
            stmt.setString(1, status.toString());
            stmt.setLong(2, orderId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateOrderStatusAndTimestamp(long orderId, OrderStatus status, String timestampColumn) {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement(
                "UPDATE orders SET status = ?, " + timestampColumn + " = ? WHERE id = ?"
            );
            stmt.setString(1, status.toString());
            stmt.setTimestamp(2, Timestamp.from(Instant.now()));
            stmt.setLong(3, orderId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setTrackingCode(long orderId, String trackingCode) {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement("UPDATE orders SET tracking_code = ? WHERE id = ?");
            stmt.setString(1, trackingCode);
            stmt.setLong(2, orderId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void recordPayment(long orderId, String transactionId, Instant paidAt) {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement(
                "INSERT INTO payment_transactions (order_id, transaction_id, paid_at) VALUES (?, ?, ?)"
            );
            stmt.setLong(1, orderId);
            stmt.setString(2, transactionId);
            stmt.setTimestamp(3, Timestamp.from(paidAt));
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.info("Payment already recorded for order {}", orderId);
        }
    }

    public boolean isPaymentAlreadyRecorded(long orderId, String transactionId) {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement(
                "SELECT 1 FROM payment_transactions WHERE order_id = ? AND transaction_id = ?"
            );
            stmt.setLong(1, orderId);
            stmt.setString(2, transactionId);
            var rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    public Instant getOrderCreatedAt(long orderId) {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement("SELECT created_at FROM orders WHERE id = ?");
            stmt.setLong(1, orderId);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getTimestamp("created_at").toInstant();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public void decrementStock(String sku, int quantity) {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement("UPDATE products SET stock = stock - ? WHERE sku = ?");
            stmt.setInt(1, quantity);
            stmt.setString(2, sku);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void incrementStock(String sku, int quantity) {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement("UPDATE products SET stock = stock + ? WHERE sku = ?");
            stmt.setInt(1, quantity);
            stmt.setString(2, sku);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int getStockForProduct(String sku) {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement("SELECT stock FROM products WHERE sku = ?");
            stmt.setString(1, sku);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("stock");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return -1;
    }

    public String getCustomerCpfForOrder(long orderId) {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement("SELECT customer_cpf FROM orders WHERE id = ?");
            stmt.setLong(1, orderId);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("customer_cpf");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
