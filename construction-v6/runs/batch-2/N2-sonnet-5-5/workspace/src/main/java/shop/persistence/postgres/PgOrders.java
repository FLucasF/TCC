package shop.persistence.postgres;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import shop.Json;
import shop.domain.CartItem;
import shop.domain.Order;
import shop.domain.OrderStatus;
import shop.domain.Summary;
import shop.persistence.OrderRepository;

class PgOrders implements OrderRepository {
    private static final String COLUMNS = "id, cpf, status, created_at, summary_json, items_json, tracking_code";

    private final Connection c;

    PgOrders(Connection c) {
        this.c = c;
    }

    @Override
    public void insert(Order o) {
        Jdbc.call(() -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO orders(" + COLUMNS
                    + ") VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, o.id());
                ps.setString(2, o.cpf());
                ps.setString(3, o.status().name());
                ps.setObject(4, OffsetDateTime.ofInstant(o.createdAt(), ZoneOffset.UTC));
                ps.setString(5, write(o.summary()));
                ps.setString(6, write(o.items()));
                ps.setString(7, o.trackingCode());
                ps.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public Optional<Order> find(String id) {
        return select(id, "");
    }

    @Override
    public Optional<Order> findForUpdate(String id) {
        return select(id, " FOR UPDATE");
    }

    private Optional<Order> select(String id, String suffix) {
        return Jdbc.call(() -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + COLUMNS + " FROM orders WHERE id = ?" + suffix)) {
                ps.setString(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new Order(rs.getString("id"),
                            OrderStatus.valueOf(rs.getString("status")),
                            rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                            read(rs.getString("summary_json"), new TypeReference<Summary>() { }),
                            read(rs.getString("items_json"), new TypeReference<List<CartItem>>() { }),
                            rs.getString("tracking_code"), rs.getString("cpf")));
                }
            }
        });
    }

    @Override
    public void update(Order o) {
        Jdbc.call(() -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE orders SET status = ?, tracking_code = ? WHERE id = ?")) {
                ps.setString(1, o.status().name());
                ps.setString(2, o.trackingCode());
                ps.setString(3, o.id());
                ps.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public boolean hasPurchased(String cpf) {
        return Jdbc.call(() -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM orders WHERE cpf = ? "
                    + "AND status IN ('PAID', 'SHIPPED', 'DELIVERED') LIMIT 1")) {
                ps.setString(1, cpf);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    private static String write(Object value) {
        try {
            return Json.MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static <T> T read(String json, TypeReference<T> type) {
        try {
            return Json.MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
