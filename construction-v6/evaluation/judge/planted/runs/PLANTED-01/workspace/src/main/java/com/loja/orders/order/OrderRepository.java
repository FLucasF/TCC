package com.loja.orders.order;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.loja.orders.checkout.CheckoutSummary;
import com.loja.orders.common.Money;
import com.loja.orders.coupon.PurchaseHistory;

@Repository
public class OrderRepository implements PurchaseHistory {

    private final JdbcClient jdbc;

    public OrderRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(Order order) {
        CheckoutSummary s = order.summary();
        jdbc.sql("""
                INSERT INTO customer_order (id, cpf, email, status, created_at, tracking_code,
                    products_subtotal, coupon_discount, shipping_cost, delivery_days, insurance,
                    payment_adjustment, final_total, installments, installment_amount,
                    next_purchase_credit, gift)
                VALUES (:id, :cpf, :email, :status, :createdAt, NULL,
                    :productsSubtotal, :couponDiscount, :shippingCost, :deliveryDays, :insurance,
                    :paymentAdjustment, :finalTotal, :installments, :installmentAmount,
                    :nextPurchaseCredit, :gift)
                """)
                .param("id", order.id())
                .param("cpf", order.cpf())
                .param("email", order.email())
                .param("status", order.status().name())
                .param("createdAt", utc(order.createdAt()))
                .param("productsSubtotal", s.productsSubtotal())
                .param("couponDiscount", s.couponDiscount())
                .param("shippingCost", s.shippingCost())
                .param("deliveryDays", s.deliveryDays())
                .param("insurance", s.insurance())
                .param("paymentAdjustment", s.paymentAdjustment())
                .param("finalTotal", s.finalTotal())
                .param("installments", s.installments())
                .param("installmentAmount", s.installmentAmount())
                .param("nextPurchaseCredit", s.nextPurchaseCredit())
                .param("gift", s.gift())
                .update();
        for (int line = 0; line < order.items().size(); line++) {
            OrderItem item = order.items().get(line);
            jdbc.sql("INSERT INTO order_item (order_id, line_no, sku, quantity) VALUES (:id, :line, :sku, :qty)")
                    .param("id", order.id())
                    .param("line", line)
                    .param("sku", item.sku())
                    .param("qty", item.quantity())
                    .update();
        }
    }

    public Optional<Order> findById(String id) {
        return find("SELECT * FROM customer_order WHERE id = :id", id);
    }

    /** Busca o pedido travando-o até o fim da transação: as operações de um pedido vão uma de cada vez. */
    public Optional<Order> lockById(String id) {
        return find("SELECT * FROM customer_order WHERE id = :id FOR UPDATE", id);
    }

    public void updateStatus(String id, OrderStatus status) {
        jdbc.sql("UPDATE customer_order SET status = :status WHERE id = :id")
                .param("status", status.name())
                .param("id", id)
                .update();
    }

    public void markPaid(String id, String transactionId, Instant paidAt) {
        jdbc.sql("""
                UPDATE customer_order SET status = :status, payment_transaction_id = :tx, paid_at = :paidAt
                WHERE id = :id
                """)
                .param("status", OrderStatus.PAID.name())
                .param("tx", transactionId)
                .param("paidAt", utc(paidAt))
                .param("id", id)
                .update();
    }

    public void markShipped(String id, String trackingCode) {
        jdbc.sql("UPDATE customer_order SET status = :status, tracking_code = :tracking WHERE id = :id")
                .param("status", OrderStatus.SHIPPED.name())
                .param("tracking", trackingCode)
                .param("id", id)
                .update();
    }

    public Optional<PaymentNoticeOutcome> findPaymentNotice(String orderId, String transactionId) {
        return jdbc.sql("SELECT outcome FROM payment_notice WHERE order_id = :id AND transaction_id = :tx")
                .param("id", orderId)
                .param("tx", transactionId)
                .query((rs, n) -> PaymentNoticeOutcome.valueOf(rs.getString("outcome")))
                .optional();
    }

    public void savePaymentNotice(String orderId, String transactionId, PaymentNoticeOutcome outcome) {
        jdbc.sql("INSERT INTO payment_notice (order_id, transaction_id, outcome) VALUES (:id, :tx, :outcome)")
                .param("id", orderId)
                .param("tx", transactionId)
                .param("outcome", outcome.name())
                .update();
    }

    @Override
    public boolean hasPurchased(String cpf) {
        return jdbc.sql("SELECT COUNT(*) FROM customer_order WHERE cpf = :cpf AND status IN (:statuses)")
                .param("cpf", cpf)
                .param("statuses", Arrays.stream(OrderStatus.values())
                        .filter(OrderStatus::paymentSettled).map(OrderStatus::name).toList())
                .query(Long.class)
                .single() > 0;
    }

    private Optional<Order> find(String sql, String id) {
        return jdbc.sql(sql).param("id", id).query(this::map).optional();
    }

    private Order map(ResultSet rs, int rowNum) throws SQLException {
        String id = rs.getString("id");
        CheckoutSummary summary = new CheckoutSummary(
                money(rs, "products_subtotal"),
                money(rs, "coupon_discount"),
                money(rs, "shipping_cost"),
                rs.getInt("delivery_days"),
                money(rs, "insurance"),
                money(rs, "payment_adjustment"),
                money(rs, "final_total"),
                rs.getInt("installments"),
                money(rs, "installment_amount"),
                money(rs, "next_purchase_credit"),
                rs.getBoolean("gift"));
        return new Order(
                id,
                rs.getString("cpf"),
                rs.getString("email"),
                OrderStatus.valueOf(rs.getString("status")),
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                summary,
                items(id),
                rs.getString("tracking_code"));
    }

    private List<OrderItem> items(String orderId) {
        return jdbc.sql("SELECT sku, quantity FROM order_item WHERE order_id = :id ORDER BY line_no")
                .param("id", orderId)
                .query((rs, n) -> new OrderItem(rs.getString("sku"), rs.getInt("quantity")))
                .list();
    }

    private static BigDecimal money(ResultSet rs, String column) throws SQLException {
        return Money.round(rs.getBigDecimal(column));
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
