package com.shop.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shop.dto.CheckoutSummary;
import com.shop.model.OrderItem;
import com.shop.model.enums.*;
import org.postgresql.util.PGobject;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Database {
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private final String url;
    private final String user;
    private final String password;

    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public void initialize() {
        try (Connection conn = getConnection()) {
            createTables(conn);
            loadInitialCatalog(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void createTables(Connection conn) throws SQLException {
        String[] sqls = {
            """
            CREATE TABLE IF NOT EXISTS products (
                sku VARCHAR(50) PRIMARY KEY,
                name VARCHAR(200) NOT NULL,
                price DECIMAL(10,2) NOT NULL,
                weight_kg DECIMAL(10,3) NOT NULL,
                stock INTEGER NOT NULL,
                discontinued BOOLEAN NOT NULL DEFAULT FALSE
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS customers (
                cpf VARCHAR(11) PRIMARY KEY,
                email VARCHAR(255) NOT NULL,
                credit_balance DECIMAL(10,2) NOT NULL DEFAULT 0
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS orders (
                id BIGSERIAL PRIMARY KEY,
                customer_cpf VARCHAR(11) NOT NULL,
                status VARCHAR(50) NOT NULL,
                created_at TIMESTAMP NOT NULL,
                summary JSONB NOT NULL,
                items JSONB NOT NULL,
                tracking_code VARCHAR(100),
                last_payment_transaction_id VARCHAR(100),
                FOREIGN KEY (customer_cpf) REFERENCES customers(cpf)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS notifications (
                id BIGSERIAL PRIMARY KEY,
                type VARCHAR(50) NOT NULL,
                order_id BIGINT NOT NULL,
                customer_cpf VARCHAR(11) NOT NULL,
                status VARCHAR(50) NOT NULL,
                FOREIGN KEY (order_id) REFERENCES orders(id),
                FOREIGN KEY (customer_cpf) REFERENCES customers(cpf)
            )
            """,
            """
            CREATE INDEX IF NOT EXISTS idx_orders_customer ON orders(customer_cpf)
            """,
            """
            CREATE INDEX IF NOT EXISTS idx_notifications_customer ON notifications(customer_cpf)
            """
        };

        for (String sql : sqls) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        }
    }

    private void loadInitialCatalog(Connection conn) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM products";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }
        }

        String insertSql = """
            INSERT INTO products (sku, name, price, weight_kg, stock, discontinued) VALUES
            ('CAM-001', 'Camiseta básica', 79.90, 0.30, 50, false),
            ('TEN-001', 'Tênis corrida', 249.90, 1.20, 10, false),
            ('FON-001', 'Fone sem fio', 199.90, 0.25, 5, false),
            ('MEI-001', 'Meia esportiva', 19.90, 0.10, 100, false),
            ('JAQ-001', 'Jaqueta corta-vento', 389.90, 0.80, 3, false),
            ('MOC-001', 'Mochila urbana', 159.90, 0.90, 1, false),
            ('BON-001', 'Boné aba reta', 59.90, 0.15, 0, false),
            ('REL-001', 'Relógio digital', 129.90, 0.20, 8, true)
            """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(insertSql);
        }
    }

    public static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize from JSON", e);
        }
    }

    public static CheckoutSummary getSummaryFromResultSet(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        return fromJson(json, CheckoutSummary.class);
    }

    public static List<OrderItem> getItemsFromResultSet(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        try {
            return objectMapper.readValue(json,
                objectMapper.getTypeFactory().constructCollectionType(List.class, OrderItem.class));
        } catch (Exception e) {
            throw new SQLException("Failed to parse items", e);
        }
    }

    public static PGobject createJsonb(Object obj) throws SQLException {
        PGobject jsonb = new PGobject();
        jsonb.setType("jsonb");
        jsonb.setValue(toJson(obj));
        return jsonb;
    }
}
