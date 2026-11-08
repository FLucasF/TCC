package com.marealta;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

public final class Db {
    @FunctionalInterface
    public interface Work<T> {
        T run(Connection c) throws SQLException;
    }

    private final DataSource ds;

    public Db(DataSource ds) {
        this.ds = ds;
    }

    public static Db connect(String url, String user, String password) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(password);
        cfg.setMaximumPoolSize(10);
        cfg.setInitializationFailTimeout(60_000); // espera o banco subir
        return new Db(new HikariDataSource(cfg));
    }

    /** Executa o trabalho numa transação: commit se terminar, rollback se lançar exceção. */
    public <T> T tx(Work<T> work) {
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (RuntimeException | SQLException | Error e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro de banco de dados", e);
        }
    }

    /** Cria o que o sistema precisa num banco vazio (idempotente). */
    public void migrate() {
        tx(c -> {
            try (Statement s = c.createStatement()) {
                s.execute("SELECT pg_advisory_xact_lock(7070)");
                s.execute("""
                        CREATE TABLE IF NOT EXISTS rooms (
                          number INTEGER PRIMARY KEY,
                          type TEXT NOT NULL,
                          capacity INTEGER NOT NULL,
                          nightly_rate NUMERIC(12,2) NOT NULL)""");
                s.execute("""
                        CREATE TABLE IF NOT EXISTS reservations (
                          id BIGSERIAL PRIMARY KEY,
                          room_number INTEGER NOT NULL REFERENCES rooms(number),
                          guest_name TEXT NOT NULL,
                          guest_email TEXT NOT NULL,
                          guest_cpf TEXT NOT NULL,
                          check_in DATE NOT NULL,
                          check_out DATE NOT NULL,
                          guests INTEGER NOT NULL,
                          rate_plan TEXT NOT NULL,
                          company_code TEXT,
                          nights INTEGER NOT NULL,
                          subtotal NUMERIC(12,2) NOT NULL,
                          discount NUMERIC(12,2) NOT NULL,
                          total NUMERIC(12,2) NOT NULL,
                          status TEXT NOT NULL,
                          amount_paid NUMERIC(12,2) NOT NULL,
                          refund NUMERIC(12,2))""");
                s.execute("CREATE INDEX IF NOT EXISTS reservations_room_idx ON reservations(room_number, check_in)");
                s.execute("""
                        CREATE TABLE IF NOT EXISTS messages (
                          id BIGSERIAL PRIMARY KEY,
                          channel TEXT NOT NULL,
                          recipient TEXT NOT NULL,
                          subject TEXT NOT NULL,
                          body TEXT NOT NULL)""");
                int[] numbers = {101, 102, 201, 202, 301};
                RoomType[] types = {RoomType.STANDARD, RoomType.STANDARD, RoomType.DELUXE, RoomType.DELUXE, RoomType.SUITE};
                for (int i = 0; i < numbers.length; i++) {
                    s.execute("INSERT INTO rooms(number,type,capacity,nightly_rate) VALUES (" + numbers[i] + ",'"
                            + types[i].name() + "'," + types[i].capacity + "," + types[i].nightlyRate
                            + ") ON CONFLICT (number) DO NOTHING");
                }
            }
            return null;
        });
    }
}
