package br.com.marealta.pousada.persistence;

import br.com.marealta.pousada.domain.Room;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Function;

public final class Database implements AutoCloseable {
    private final HikariDataSource dataSource;

    /** @param schema schema a usar; {@code null} usa o padrão do usuário. */
    public Database(String url, String user, String password, String schema) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(password);
        cfg.setMaximumPoolSize(10);
        cfg.setInitializationFailTimeout(30_000);
        if (schema != null) {
            cfg.setSchema(schema);
        }
        this.dataSource = new HikariDataSource(cfg);
    }

    /** Cria o que falta no banco e grava os quartos. */
    public void migrate() {
        inTransaction(tx -> {
            try (Statement st = tx.connection().createStatement()) {
                st.execute("SELECT pg_advisory_xact_lock(727001)");
                st.execute("""
                        CREATE TABLE IF NOT EXISTS rooms (
                            number INTEGER PRIMARY KEY,
                            type VARCHAR(20) NOT NULL,
                            capacity INTEGER NOT NULL,
                            nightly_rate NUMERIC(12,2) NOT NULL)""");
                st.execute("""
                        CREATE TABLE IF NOT EXISTS reservations (
                            id BIGSERIAL PRIMARY KEY,
                            room_number INTEGER NOT NULL REFERENCES rooms(number),
                            guest_name TEXT NOT NULL,
                            guest_email TEXT NOT NULL,
                            guest_cpf TEXT NOT NULL,
                            check_in DATE NOT NULL,
                            check_out DATE NOT NULL,
                            guests INTEGER NOT NULL,
                            rate_plan VARCHAR(20) NOT NULL,
                            nights INTEGER NOT NULL,
                            subtotal NUMERIC(12,2) NOT NULL,
                            discount NUMERIC(12,2) NOT NULL,
                            total NUMERIC(12,2) NOT NULL,
                            status VARCHAR(20) NOT NULL,
                            amount_paid NUMERIC(12,2) NOT NULL,
                            refund NUMERIC(12,2))""");
                st.execute("CREATE INDEX IF NOT EXISTS reservations_room_dates ON reservations (room_number, check_in, check_out)");
                st.execute("""
                        CREATE TABLE IF NOT EXISTS messages (
                            id BIGSERIAL PRIMARY KEY,
                            channel VARCHAR(20) NOT NULL,
                            recipient TEXT NOT NULL,
                            subject TEXT NOT NULL,
                            body TEXT NOT NULL)""");
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
            for (Room room : Room.ALL) {
                tx.rooms().insertIfAbsent(room);
            }
            return null;
        });
    }

    public <T> T inTransaction(Function<Tx, T> work) {
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.apply(new Tx(c));
                c.commit();
                return result;
            } catch (RuntimeException | Error e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro de banco de dados", e);
        }
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
