package shop.persistence;

import shop.catalog.InitialCatalog;
import shop.catalog.Product;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/** Creates what the service needs in an empty database and loads the initial catalog. Safe to run on two machines at once. */
public final class Schema {
    private static final long LOCK_KEY = 7_340_011L;

    private static final List<String> DDL = List.of(
            """
            CREATE TABLE IF NOT EXISTS products (
                sku text PRIMARY KEY,
                name text NOT NULL,
                price numeric(12,2) NOT NULL,
                weight_kg numeric(10,2) NOT NULL,
                stock integer NOT NULL CHECK (stock >= 0),
                discontinued boolean NOT NULL)""",
            """
            CREATE TABLE IF NOT EXISTS customers (
                cpf text PRIMARY KEY,
                credit_balance numeric(12,2) NOT NULL DEFAULT 0)""",
            """
            CREATE TABLE IF NOT EXISTS orders (
                id text PRIMARY KEY,
                customer_cpf text NOT NULL REFERENCES customers(cpf),
                email text NOT NULL,
                status text NOT NULL,
                created_at timestamptz NOT NULL,
                summary text NOT NULL,
                tracking_code text)""",
            "CREATE INDEX IF NOT EXISTS orders_customer_idx ON orders (customer_cpf, status)",
            """
            CREATE TABLE IF NOT EXISTS order_items (
                order_id text NOT NULL REFERENCES orders(id),
                sku text NOT NULL,
                quantity integer NOT NULL,
                PRIMARY KEY (order_id, sku))""",
            """
            CREATE TABLE IF NOT EXISTS notifications (
                seq bigserial PRIMARY KEY,
                id text NOT NULL UNIQUE,
                customer_cpf text NOT NULL,
                type text NOT NULL,
                order_id text NOT NULL,
                status text NOT NULL)""",
            "CREATE INDEX IF NOT EXISTS notifications_customer_idx ON notifications (customer_cpf, seq)",
            """
            CREATE TABLE IF NOT EXISTS payment_notices (
                order_id text NOT NULL,
                transaction_id text NOT NULL,
                refusal text,
                PRIMARY KEY (order_id, transaction_id))""");

    private Schema() {
    }

    public static void init(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(true);
            try (PreparedStatement lock = connection.prepareStatement("SELECT pg_advisory_lock(?)")) {
                lock.setLong(1, LOCK_KEY);
                lock.execute();
            }
            try {
                try (Statement statement = connection.createStatement()) {
                    for (String ddl : DDL) {
                        statement.execute(ddl);
                    }
                }
                for (Product product : InitialCatalog.PRODUCTS) {
                    try (PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO products (sku, name, price, weight_kg, stock, discontinued) "
                                    + "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (sku) DO NOTHING")) {
                        insert.setString(1, product.sku());
                        insert.setString(2, product.name());
                        insert.setBigDecimal(3, product.price());
                        insert.setBigDecimal(4, product.weightKg());
                        insert.setInt(5, product.stock());
                        insert.setBoolean(6, product.discontinued());
                        insert.executeUpdate();
                    }
                }
            } finally {
                try (PreparedStatement unlock = connection.prepareStatement("SELECT pg_advisory_unlock(?)")) {
                    unlock.setLong(1, LOCK_KEY);
                    unlock.execute();
                }
            }
        }
    }
}
