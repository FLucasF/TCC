package shop.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;

/** Creates the tables and the initial catalog. Safe to run on every start, from several instances at once. */
public final class Schema {
    private static final long LOCK_KEY = 7_272_018L;

    private static final List<String> DDL = List.of(
            "CREATE TABLE IF NOT EXISTS products ("
                    + "sku VARCHAR(64) PRIMARY KEY, name VARCHAR(255) NOT NULL, price NUMERIC(12,2) NOT NULL, "
                    + "weight_kg NUMERIC(10,2) NOT NULL, stock INTEGER NOT NULL CHECK (stock >= 0), "
                    + "discontinued BOOLEAN NOT NULL)",
            "CREATE TABLE IF NOT EXISTS customers ("
                    + "cpf VARCHAR(32) PRIMARY KEY, email VARCHAR(255) NOT NULL, "
                    + "credit_balance NUMERIC(12,2) NOT NULL DEFAULT 0)",
            "CREATE TABLE IF NOT EXISTS orders ("
                    + "id VARCHAR(36) PRIMARY KEY, cpf VARCHAR(32) NOT NULL, email VARCHAR(255) NOT NULL, "
                    + "status VARCHAR(32) NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL, "
                    + "summary TEXT NOT NULL, tracking_code VARCHAR(255))",
            "CREATE INDEX IF NOT EXISTS idx_orders_cpf_status ON orders (cpf, status)",
            "CREATE TABLE IF NOT EXISTS order_items ("
                    + "order_id VARCHAR(36) NOT NULL REFERENCES orders (id), line_no INTEGER NOT NULL, "
                    + "sku VARCHAR(64) NOT NULL, quantity INTEGER NOT NULL, PRIMARY KEY (order_id, line_no))",
            "CREATE TABLE IF NOT EXISTS payment_events ("
                    + "order_id VARCHAR(36) NOT NULL, transaction_id VARCHAR(255) NOT NULL, "
                    + "outcome VARCHAR(32) NOT NULL, PRIMARY KEY (order_id, transaction_id))",
            "CREATE TABLE IF NOT EXISTS notifications ("
                    + "seq BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, id VARCHAR(36) NOT NULL UNIQUE, "
                    + "cpf VARCHAR(32) NOT NULL, notif_type VARCHAR(32) NOT NULL, order_id VARCHAR(36) NOT NULL, "
                    + "status VARCHAR(8) NOT NULL)",
            "CREATE INDEX IF NOT EXISTS idx_notifications_cpf ON notifications (cpf)");

    private static final String[][] CATALOG = {
            {"CAM-001", "Camiseta básica", "79.90", "0.30", "50", "false"},
            {"TEN-001", "Tênis corrida", "249.90", "1.20", "10", "false"},
            {"FON-001", "Fone sem fio", "199.90", "0.25", "5", "false"},
            {"MEI-001", "Meia esportiva", "19.90", "0.10", "100", "false"},
            {"JAQ-001", "Jaqueta corta-vento", "389.90", "0.80", "3", "false"},
            {"MOC-001", "Mochila urbana", "159.90", "0.90", "1", "false"},
            {"BON-001", "Boné aba reta", "59.90", "0.15", "0", "false"},
            {"REL-001", "Relógio digital", "129.90", "0.20", "8", "true"}};

    private Schema() {
    }

    public static void init(DataSource dataSource) throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(true);
            boolean postgres = c.getMetaData().getDatabaseProductName().toLowerCase().contains("postgres");
            if (postgres) {
                // serializes concurrent starts of several instances
                try (Statement s = c.createStatement()) {
                    s.execute("SELECT pg_advisory_lock(" + LOCK_KEY + ")");
                }
            }
            try {
                try (Statement s = c.createStatement()) {
                    for (String ddl : DDL) {
                        s.execute(ddl);
                    }
                }
                seedCatalog(c);
            } finally {
                if (postgres) {
                    try (Statement s = c.createStatement()) {
                        s.execute("SELECT pg_advisory_unlock(" + LOCK_KEY + ")");
                    }
                }
            }
        }
    }

    private static void seedCatalog(Connection c) throws SQLException {
        String sql = "INSERT INTO products (sku, name, price, weight_kg, stock, discontinued) "
                + "SELECT CAST(? AS VARCHAR(64)), CAST(? AS VARCHAR(255)), CAST(? AS NUMERIC(12,2)), "
                + "CAST(? AS NUMERIC(10,2)), CAST(? AS INTEGER), CAST(? AS BOOLEAN) "
                + "WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = ?)";
        try (var ps = c.prepareStatement(sql)) {
            for (String[] row : CATALOG) {
                ps.setString(1, row[0]);
                ps.setString(2, row[1]);
                ps.setBigDecimal(3, new java.math.BigDecimal(row[2]));
                ps.setBigDecimal(4, new java.math.BigDecimal(row[3]));
                ps.setInt(5, Integer.parseInt(row[4]));
                ps.setBoolean(6, Boolean.parseBoolean(row[5]));
                ps.setString(7, row[0]);
                ps.executeUpdate();
            }
        }
    }
}
