package shop.persistence.postgres;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.function.Function;
import javax.sql.DataSource;
import shop.persistence.Store;
import shop.persistence.Tx;

public class PostgresStore implements Store {
    private static final long MIGRATION_LOCK = 727_274L;

    private static final List<String> SCHEMA = List.of(
            """
            CREATE TABLE IF NOT EXISTS products (
              sku text PRIMARY KEY,
              name text NOT NULL,
              price numeric(12,2) NOT NULL,
              weight_kg numeric(10,3) NOT NULL,
              stock integer NOT NULL CHECK (stock >= 0),
              discontinued boolean NOT NULL)""",
            """
            CREATE TABLE IF NOT EXISTS customers (
              cpf text PRIMARY KEY,
              credit_balance numeric(12,2) NOT NULL DEFAULT 0)""",
            """
            CREATE TABLE IF NOT EXISTS orders (
              id text PRIMARY KEY,
              cpf text NOT NULL REFERENCES customers(cpf),
              status text NOT NULL,
              created_at timestamptz NOT NULL,
              summary_json text NOT NULL,
              items_json text NOT NULL,
              tracking_code text)""",
            "CREATE INDEX IF NOT EXISTS orders_cpf_idx ON orders(cpf)",
            """
            CREATE TABLE IF NOT EXISTS payments (
              order_id text NOT NULL,
              transaction_id text NOT NULL,
              error_code text,
              PRIMARY KEY (order_id, transaction_id))""",
            """
            CREATE TABLE IF NOT EXISTS notifications (
              seq bigserial PRIMARY KEY,
              id text NOT NULL UNIQUE,
              cpf text NOT NULL,
              type text NOT NULL,
              order_id text NOT NULL,
              status text NOT NULL)""",
            "CREATE INDEX IF NOT EXISTS notifications_cpf_idx ON notifications(cpf)");

    private final DataSource dataSource;

    public PostgresStore(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Cria as tabelas; o lock evita corrida quando as duas máquinas sobem juntas. */
    public void migrate() {
        Jdbc.call(() -> {
            try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
                s.execute("SELECT pg_advisory_lock(" + MIGRATION_LOCK + ")");
                try {
                    for (String ddl : SCHEMA) {
                        s.execute(ddl);
                    }
                } finally {
                    s.execute("SELECT pg_advisory_unlock(" + MIGRATION_LOCK + ")");
                }
            }
            return null;
        });
    }

    @Override
    public <T> T inTransaction(Function<Tx, T> work) {
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.apply(new PostgresTx(c));
                c.commit();
                return result;
            } catch (RuntimeException | Error e) {
                try {
                    c.rollback();
                } catch (SQLException suppressed) {
                    e.addSuppressed(suppressed);
                }
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
