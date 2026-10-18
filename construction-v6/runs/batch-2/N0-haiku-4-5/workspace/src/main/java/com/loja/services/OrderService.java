package com.loja.services;

import com.loja.api.CheckoutRequest;
import com.loja.database.Database;
import com.loja.models.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;

public class OrderService {
    public static Order createOrder(CheckoutRequest req, OrderSummary summary) throws SQLException {
        String orderId = UUID.randomUUID().toString();
        Instant now = Instant.now();

        CustomerService.getOrCreateCustomer(req.customer.cpf, req.customer.email);

        String sql = """
            INSERT INTO orders (id, cpf, email, status, created_at, products_subtotal, coupon_discount,
                               shipping_cost, delivery_days, insurance, payment_adjustment, final_total,
                               installments, installment_amount, next_purchase_credit, gift,
                               region, shipping_method, payment_method, club_tier, coupon, paid_at, payment_transaction_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL)
            """;

        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, orderId);
            pstmt.setString(2, req.customer.cpf);
            pstmt.setString(3, req.customer.email);
            pstmt.setString(4, Enums.OrderStatus.AWAITING_PAYMENT.name());
            pstmt.setObject(5, now);
            pstmt.setDouble(6, summary.productsSubtotal);
            pstmt.setDouble(7, summary.couponDiscount);
            pstmt.setDouble(8, summary.shippingCost);
            pstmt.setInt(9, summary.deliveryDays);
            pstmt.setDouble(10, summary.insurance);
            pstmt.setDouble(11, summary.paymentAdjustment);
            pstmt.setDouble(12, summary.finalTotal);
            pstmt.setInt(13, summary.installments);
            pstmt.setDouble(14, summary.installmentAmount);
            pstmt.setDouble(15, summary.nextPurchaseCredit);
            pstmt.setBoolean(16, summary.gift);
            pstmt.setString(17, req.region);
            pstmt.setString(18, req.shippingMethod);
            pstmt.setString(19, req.paymentMethod);
            pstmt.setString(20, req.clubTier);
            pstmt.setString(21, req.coupon != null && !req.coupon.isEmpty() ? req.coupon : null);
            pstmt.executeUpdate();
        }

        for (CheckoutRequest.OrderItemData item : req.items) {
            String itemSql = "INSERT INTO order_items (order_id, sku, quantity) VALUES (?, ?, ?)";
            try (Connection conn = Database.getDataSource().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(itemSql)) {
                pstmt.setString(1, orderId);
                pstmt.setString(2, item.sku);
                pstmt.setInt(3, item.quantity);
                pstmt.executeUpdate();
            }
        }

        return getOrder(orderId);
    }

    public static Order getOrder(String orderId) throws SQLException {
        String sql = """
            SELECT id, cpf, status, created_at, products_subtotal, coupon_discount, shipping_cost,
                   delivery_days, insurance, payment_adjustment, final_total, installments,
                   installment_amount, next_purchase_credit, gift, tracking_code
            FROM orders WHERE id = ?
            """;

        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, orderId);
            ResultSet rs = pstmt.executeQuery();

            if (!rs.next()) {
                return null;
            }

            OrderSummary summary = new OrderSummary(
                rs.getDouble("products_subtotal"),
                rs.getDouble("coupon_discount"),
                rs.getDouble("shipping_cost"),
                rs.getInt("delivery_days"),
                rs.getDouble("insurance"),
                rs.getDouble("payment_adjustment"),
                rs.getDouble("final_total"),
                rs.getInt("installments"),
                rs.getDouble("installment_amount"),
                rs.getDouble("next_purchase_credit"),
                rs.getBoolean("gift")
            );

            List<OrderItem> items = getOrderItems(orderId);

            String trackingCode = rs.getString("tracking_code");

            return new Order(
                rs.getString("id"),
                Enums.OrderStatus.valueOf(rs.getString("status")),
                rs.getObject("created_at", java.time.Instant.class),
                summary,
                items,
                trackingCode
            );
        }
    }

    private static List<OrderItem> getOrderItems(String orderId) throws SQLException {
        String sql = "SELECT sku, quantity FROM order_items WHERE order_id = ?";
        List<OrderItem> items = new ArrayList<>();

        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, orderId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                items.add(new OrderItem(rs.getString("sku"), rs.getInt("quantity")));
            }
        }
        return items;
    }

    public static void updateOrderStatus(String orderId, Enums.OrderStatus status) throws SQLException {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setString(2, orderId);
            pstmt.executeUpdate();
        }
    }

    public static void updateOrderTracking(String orderId, String trackingCode) throws SQLException {
        String sql = "UPDATE orders SET tracking_code = ? WHERE id = ?";
        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, trackingCode);
            pstmt.setString(2, orderId);
            pstmt.executeUpdate();
        }
    }

    public static void recordPayment(String orderId, String transactionId, Instant paidAt) throws SQLException {
        String sql = "UPDATE orders SET paid_at = ?, payment_transaction_id = ? WHERE id = ?";
        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, paidAt);
            pstmt.setString(2, transactionId);
            pstmt.setString(3, orderId);
            pstmt.executeUpdate();
        }
    }

    public static String getCustomerCpf(String orderId) throws SQLException {
        String sql = "SELECT cpf FROM orders WHERE id = ?";
        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, orderId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("cpf");
            }
            return null;
        }
    }

    public static boolean hasBeenPaid(String orderId) throws SQLException {
        String sql = "SELECT payment_transaction_id FROM orders WHERE id = ?";
        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, orderId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("payment_transaction_id") != null;
            }
            return false;
        }
    }

    public static boolean isPaymentDuplicate(String orderId, String transactionId) throws SQLException {
        String sql = "SELECT payment_transaction_id FROM orders WHERE id = ?";
        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, orderId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String existing = rs.getString("payment_transaction_id");
                return existing != null && existing.equals(transactionId);
            }
            return false;
        }
    }
}
