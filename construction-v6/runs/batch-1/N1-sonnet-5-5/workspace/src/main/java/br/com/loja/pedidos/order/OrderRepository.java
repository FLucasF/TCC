package br.com.loja.pedidos.order;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.com.loja.pedidos.checkout.Summary;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class OrderRepository {

    private static final String COLUMNS = "id, cpf, email, status, created_at, tracking_code, summary_json";

    private final JdbcClient jdbc;
    private final JsonMapper mapper;

    public OrderRepository(JdbcClient jdbc, JsonMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public void insert(Order order) {
        jdbc.sql("INSERT INTO orders (" + COLUMNS + ") VALUES (:id, :cpf, :email, :status, :createdAt, :trackingCode, :summary)")
                .param("id", order.id())
                .param("cpf", order.cpf())
                .param("email", order.email())
                .param("status", order.status().name())
                .param("createdAt", OffsetDateTime.ofInstant(order.createdAt(), ZoneOffset.UTC))
                .param("trackingCode", order.trackingCode(), java.sql.Types.VARCHAR)
                .param("summary", mapper.writeValueAsString(order.summary()))
                .update();
        for (int i = 0; i < order.items().size(); i++) {
            OrderItem item = order.items().get(i);
            jdbc.sql("INSERT INTO order_items (order_id, sku, quantity, position) VALUES (?, ?, ?, ?)")
                    .param(order.id()).param(item.sku()).param(item.quantity()).param(i).update();
        }
    }

    public Optional<Order> find(String id) {
        return findWith("SELECT " + COLUMNS + " FROM orders WHERE id = ?", id);
    }

    /** Trava o pedido até o fim da transação, serializando operações concorrentes sobre ele. */
    public Optional<Order> findForUpdate(String id) {
        return findWith("SELECT " + COLUMNS + " FROM orders WHERE id = ? FOR UPDATE", id);
    }

    public void update(String id, OrderStatus status, String trackingCode) {
        jdbc.sql("UPDATE orders SET status = :status, tracking_code = :trackingCode WHERE id = :id")
                .param("status", status.name())
                .param("trackingCode", trackingCode, java.sql.Types.VARCHAR)
                .param("id", id)
                .update();
    }

    /** O CPF já tem pedido com pagamento confirmado (pago, enviado ou entregue)? */
    public boolean hasPurchase(String cpf) {
        List<String> statuses = OrderStatus.withPaymentReceived().stream().map(Enum::name).toList();
        String placeholders = statuses.stream().map(s -> "?").collect(Collectors.joining(", "));
        var statement = jdbc.sql("SELECT COUNT(*) FROM orders WHERE cpf = ? AND status IN (" + placeholders + ")")
                .param(cpf);
        for (String status : statuses) {
            statement = statement.param(status);
        }
        return statement.query(Long.class).single() > 0;
    }

    public Optional<String> findPaymentNotice(String orderId, String transactionId) {
        return jdbc.sql("SELECT COALESCE(error_code, '') FROM payment_notices WHERE order_id = ? AND transaction_id = ?")
                .param(orderId).param(transactionId)
                .query(String.class)
                .optional();
    }

    /** Guarda o resultado do aviso de pagamento; errorCode vazio significa que foi aceito. */
    public void savePaymentNotice(String orderId, String transactionId, String errorCode) {
        jdbc.sql("INSERT INTO payment_notices (order_id, transaction_id, error_code) VALUES (:orderId, :transactionId, :errorCode)")
                .param("orderId", orderId).param("transactionId", transactionId)
                .param("errorCode", errorCode, java.sql.Types.VARCHAR)
                .update();
    }

    private Optional<Order> findWith(String sql, String id) {
        return jdbc.sql(sql).param(id)
                .query((rs, n) -> new Order(
                        rs.getString("id"),
                        OrderStatus.valueOf(rs.getString("status")),
                        rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                        mapper.readValue(rs.getString("summary_json"), Summary.class),
                        itemsOf(id),
                        rs.getString("tracking_code"),
                        rs.getString("cpf"),
                        rs.getString("email")))
                .optional();
    }

    private List<OrderItem> itemsOf(String orderId) {
        return jdbc.sql("SELECT sku, quantity FROM order_items WHERE order_id = ? ORDER BY position")
                .param(orderId)
                .query((rs, n) -> new OrderItem(rs.getString("sku"), rs.getInt("quantity")))
                .list();
    }
}
