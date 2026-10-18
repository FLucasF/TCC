package br.com.loja.pedidos.order;

import br.com.loja.pedidos.checkout.OrderSummary;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository {

    private static final String SELECT = "SELECT * FROM orders WHERE id = :id";

    private final JdbcClient jdbc;

    public OrderRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(String cpf, String email, Order order) {
        OrderSummary s = order.summary();
        jdbc.sql("""
                INSERT INTO orders (id, cpf, email, status, created_at, tracking_code, products_subtotal,
                  coupon_discount, shipping_cost, delivery_days, insurance, payment_adjustment, final_total,
                  installments, installment_amount, next_purchase_credit, gift)
                VALUES (:id, :cpf, :email, :status, :createdAt, NULL, :subtotal, :discount, :shipping, :days,
                  :insurance, :adjustment, :total, :installments, :installmentAmount, :credit, :gift)
                """)
                .param("id", order.id()).param("cpf", cpf).param("email", email)
                .param("status", order.status().name())
                .param("createdAt", OffsetDateTime.ofInstant(order.createdAt(), ZoneOffset.UTC))
                .param("subtotal", s.productsSubtotal()).param("discount", s.couponDiscount())
                .param("shipping", s.shippingCost()).param("days", s.deliveryDays())
                .param("insurance", s.insurance()).param("adjustment", s.paymentAdjustment())
                .param("total", s.finalTotal()).param("installments", s.installments())
                .param("installmentAmount", s.installmentAmount()).param("credit", s.nextPurchaseCredit())
                .param("gift", s.gift()).update();
        int position = 0;
        for (Order.Item item : order.items()) {
            jdbc.sql("INSERT INTO order_items (order_id, position, sku, quantity) VALUES (:id, :pos, :sku, :q)")
                    .param("id", order.id()).param("pos", position++).param("sku", item.sku())
                    .param("q", item.quantity()).update();
        }
    }

    public Optional<StoredOrder> find(String id) {
        return jdbc.sql(SELECT).param("id", id).query(this::map).optional().map(this::withItems);
    }

    /** Reads the order locking its row until the end of the transaction. */
    public Optional<StoredOrder> findForUpdate(String id) {
        return jdbc.sql(SELECT + " FOR UPDATE").param("id", id).query(this::map).optional().map(this::withItems);
    }

    public void updateStatus(String id, OrderStatus status) {
        jdbc.sql("UPDATE orders SET status = :s WHERE id = :id").param("s", status.name()).param("id", id).update();
    }

    public void updateShipment(String id, String trackingCode) {
        jdbc.sql("UPDATE orders SET status = :s, tracking_code = :t WHERE id = :id")
                .param("s", OrderStatus.SHIPPED.name()).param("t", trackingCode).param("id", id).update();
    }

    /** Whether the customer has an order that was paid, shipped or delivered. */
    public boolean hasPaidOrder(String cpf) {
        return jdbc.sql("SELECT COUNT(*) FROM orders WHERE cpf = :cpf AND status IN ('PAID', 'SHIPPED', 'DELIVERED')")
                .param("cpf", cpf).query(Long.class).single() > 0;
    }

    public Optional<String> findPaymentOutcome(String orderId, String transactionId) {
        return jdbc.sql("SELECT outcome FROM payment_events WHERE order_id = :o AND transaction_id = :t")
                .param("o", orderId).param("t", transactionId).query(String.class).optional();
    }

    public void recordPaymentOutcome(String orderId, String transactionId, String outcome) {
        jdbc.sql("INSERT INTO payment_events (order_id, transaction_id, outcome) VALUES (:o, :t, :out)")
                .param("o", orderId).param("t", transactionId).param("out", outcome).update();
    }

    private StoredOrder withItems(StoredOrder stored) {
        Order o = stored.order();
        List<Order.Item> items = jdbc.sql("SELECT sku, quantity FROM order_items WHERE order_id = :id ORDER BY position")
                .param("id", o.id()).query((rs, n) -> new Order.Item(rs.getString("sku"), rs.getInt("quantity")))
                .list();
        return new StoredOrder(stored.cpf(), new Order(o.id(), o.status(), o.createdAt(), o.summary(), items,
                o.trackingCode()));
    }

    private StoredOrder map(ResultSet rs, int row) throws SQLException {
        OrderSummary summary = new OrderSummary(money(rs, "products_subtotal"), money(rs, "coupon_discount"),
                money(rs, "shipping_cost"), rs.getInt("delivery_days"), money(rs, "insurance"),
                money(rs, "payment_adjustment"), money(rs, "final_total"), rs.getInt("installments"),
                money(rs, "installment_amount"), money(rs, "next_purchase_credit"), rs.getBoolean("gift"));
        Instant createdAt = rs.getObject("created_at", OffsetDateTime.class).toInstant();
        Order order = new Order(rs.getString("id"), OrderStatus.valueOf(rs.getString("status")), createdAt, summary,
                List.of(), rs.getString("tracking_code"));
        return new StoredOrder(rs.getString("cpf"), order);
    }

    private static java.math.BigDecimal money(ResultSet rs, String column) throws SQLException {
        return rs.getBigDecimal(column).setScale(2);
    }
}
