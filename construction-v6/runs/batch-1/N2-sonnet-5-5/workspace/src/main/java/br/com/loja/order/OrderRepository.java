package br.com.loja.order;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.com.loja.checkout.OrderSummary;

@Repository
public class OrderRepository {

    private static final String COLUMNS = """
            id, customer_cpf, status, created_at, tracking_code, payment_transaction_id, payment_outcome,
            products_subtotal, coupon_discount, shipping_cost, delivery_days, insurance, payment_adjustment,
            final_total, installments, installment_amount, next_purchase_credit, gift""";

    private final JdbcClient jdbc;

    OrderRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(Order o) {
        OrderSummary s = o.summary();
        jdbc.sql("""
                INSERT INTO orders (id, customer_cpf, status, created_at, products_subtotal, coupon_discount,
                    shipping_cost, delivery_days, insurance, payment_adjustment, final_total, installments,
                    installment_amount, next_purchase_credit, gift)
                VALUES (:id, :cpf, :status, :createdAt, :subtotal, :discount, :shipping, :days, :insurance,
                    :adjustment, :total, :installments, :installment, :credit, :gift)""")
                .param("id", o.id()).param("cpf", o.customerCpf()).param("status", o.status().name())
                .param("createdAt", OffsetDateTime.ofInstant(o.createdAt(), ZoneOffset.UTC))
                .param("subtotal", s.productsSubtotal()).param("discount", s.couponDiscount())
                .param("shipping", s.shippingCost()).param("days", s.deliveryDays())
                .param("insurance", s.insurance()).param("adjustment", s.paymentAdjustment())
                .param("total", s.finalTotal()).param("installments", s.installments())
                .param("installment", s.installmentAmount()).param("credit", s.nextPurchaseCredit())
                .param("gift", s.gift()).update();
        int line = 0;
        for (OrderItem item : o.items()) {
            jdbc.sql("INSERT INTO order_items (order_id, line_no, sku, quantity) VALUES (:id, :line, :sku, :q)")
                    .param("id", o.id()).param("line", line++).param("sku", item.sku())
                    .param("q", item.quantity()).update();
        }
    }

    public Optional<Order> findById(String id) {
        return find("SELECT " + COLUMNS + " FROM orders WHERE id = :id", id);
    }

    /** Trava a linha do pedido até o fim da transação (serializa operações entre as duas máquinas). */
    public Optional<Order> findByIdForUpdate(String id) {
        return find("SELECT " + COLUMNS + " FROM orders WHERE id = :id FOR UPDATE", id);
    }

    private Optional<Order> find(String sql, String id) {
        return jdbc.sql(sql).param("id", id).query((rs, n) -> {
            List<OrderItem> items = jdbc
                    .sql("SELECT sku, quantity FROM order_items WHERE order_id = :id ORDER BY line_no")
                    .param("id", id).query((r, i) -> new OrderItem(r.getString("sku"), r.getInt("quantity")))
                    .list();
            OrderSummary summary = new OrderSummary(money(rs, "products_subtotal"), money(rs, "coupon_discount"),
                    money(rs, "shipping_cost"), rs.getInt("delivery_days"), money(rs, "insurance"),
                    money(rs, "payment_adjustment"), money(rs, "final_total"), rs.getInt("installments"),
                    money(rs, "installment_amount"), money(rs, "next_purchase_credit"), rs.getBoolean("gift"));
            Instant createdAt = rs.getObject("created_at", OffsetDateTime.class).toInstant();
            return new Order(rs.getString("id"), rs.getString("customer_cpf"),
                    OrderStatus.valueOf(rs.getString("status")), createdAt, summary, items,
                    rs.getString("tracking_code"), rs.getString("payment_transaction_id"),
                    rs.getString("payment_outcome"));
        }).optional();
    }

    private static java.math.BigDecimal money(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        return rs.getBigDecimal(column).setScale(2);
    }

    public void updateStatus(String id, OrderStatus status) {
        jdbc.sql("UPDATE orders SET status = :status WHERE id = :id")
                .param("status", status.name()).param("id", id).update();
    }

    public void recordPayment(String id, String transactionId, String outcome, OrderStatus status) {
        jdbc.sql("""
                UPDATE orders SET status = :status, payment_transaction_id = :tx, payment_outcome = :outcome
                WHERE id = :id""")
                .param("status", status.name()).param("tx", transactionId).param("outcome", outcome)
                .param("id", id).update();
    }

    public void recordShipment(String id, String trackingCode) {
        jdbc.sql("UPDATE orders SET status = :status, tracking_code = :code WHERE id = :id")
                .param("status", OrderStatus.SHIPPED.name()).param("code", trackingCode).param("id", id).update();
    }

    /** Já comprou de verdade: tem pedido pago, enviado ou entregue (cancelado não conta). */
    public boolean hasPurchaseByCustomer(String cpf) {
        return jdbc.sql("""
                SELECT COUNT(*) FROM orders
                WHERE customer_cpf = :cpf AND status IN ('PAID', 'SHIPPED', 'DELIVERED')""")
                .param("cpf", cpf).query(Long.class).single() > 0;
    }
}
