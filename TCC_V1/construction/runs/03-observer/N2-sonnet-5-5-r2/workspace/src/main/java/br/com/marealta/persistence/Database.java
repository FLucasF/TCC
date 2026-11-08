package br.com.marealta.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.sql.DataSource;

/** Acesso ao PostgreSQL: cria as tabelas e deixa várias gravações valerem juntas numa transação. */
public final class Database {

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    @FunctionalInterface
    public interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private final DataSource dataSource;
    private final ThreadLocal<Connection> current = new ThreadLocal<>();

    public Database(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void createSchema() {
        update("""
                CREATE TABLE IF NOT EXISTS reservation (
                    id BIGSERIAL PRIMARY KEY,
                    room_number INT NOT NULL,
                    guest_name TEXT NOT NULL,
                    guest_email TEXT NOT NULL,
                    guest_cpf TEXT NOT NULL,
                    check_in DATE NOT NULL,
                    check_out DATE NOT NULL,
                    guests INT NOT NULL,
                    rate_plan TEXT NOT NULL,
                    nights INT NOT NULL,
                    subtotal NUMERIC(12,2) NOT NULL,
                    discount NUMERIC(12,2) NOT NULL,
                    total NUMERIC(12,2) NOT NULL,
                    status TEXT NOT NULL,
                    amount_paid NUMERIC(12,2) NOT NULL,
                    refund NUMERIC(12,2)
                )""", ps -> { });
        update("CREATE INDEX IF NOT EXISTS reservation_room_idx ON reservation (room_number, check_in)", ps -> { });
        update("""
                CREATE TABLE IF NOT EXISTS message (
                    id BIGSERIAL PRIMARY KEY,
                    channel TEXT NOT NULL,
                    recipient TEXT NOT NULL,
                    subject TEXT NOT NULL,
                    body TEXT NOT NULL
                )""", ps -> { });
    }

    public <T> T transaction(Supplier<T> work) {
        if (current.get() != null) {
            return work.get();
        }
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            current.set(connection);
            try {
                T result = work.get();
                connection.commit();
                return result;
            } catch (RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                current.remove();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public <T> List<T> query(String sql, Binder binder, RowMapper<T> mapper) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                binder.bind(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    List<T> rows = new ArrayList<>();
                    while (rs.next()) {
                        rows.add(mapper.map(rs));
                    }
                    return rows;
                }
            }
        });
    }

    public int update(String sql, Binder binder) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                binder.bind(ps);
                return ps.executeUpdate();
            }
        });
    }

    /** Executa um INSERT ... RETURNING id. */
    public long insert(String sql, Binder binder) {
        return query(sql, binder, rs -> rs.getLong(1)).get(0);
    }

    @FunctionalInterface
    private interface ConnectionWork<T> {
        T run(Connection connection) throws SQLException;
    }

    private <T> T withConnection(ConnectionWork<T> work) {
        Connection inTransaction = current.get();
        try {
            if (inTransaction != null) {
                return work.run(inTransaction);
            }
            try (Connection connection = dataSource.getConnection()) {
                return work.run(connection);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
