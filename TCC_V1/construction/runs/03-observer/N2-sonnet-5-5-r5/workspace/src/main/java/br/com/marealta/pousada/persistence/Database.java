package br.com.marealta.pousada.persistence;

import br.com.marealta.pousada.domain.RoomType;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

/** Conexões com o PostgreSQL e criação do que o sistema precisa. */
public class Database {
    private static final Map<Integer, RoomType> ROOMS = Map.of(
            101, RoomType.STANDARD, 102, RoomType.STANDARD,
            201, RoomType.DELUXE, 202, RoomType.DELUXE,
            301, RoomType.SUITE);

    private final String url;
    private final String user;
    private final String password;

    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public void migrate() {
        try (Connection c = connect(); Statement s = c.createStatement()) {
            // Evita que duas instâncias subindo juntas disputem a criação das tabelas.
            c.setAutoCommit(false);
            s.execute("SELECT pg_advisory_xact_lock(7070001)");
            s.execute("""
                    CREATE TABLE IF NOT EXISTS rooms (
                        number INTEGER PRIMARY KEY,
                        type   VARCHAR(20) NOT NULL
                    )""");
            s.execute("""
                    CREATE TABLE IF NOT EXISTS reservations (
                        id           BIGSERIAL PRIMARY KEY,
                        room_number  INTEGER NOT NULL REFERENCES rooms(number),
                        guest_name   TEXT NOT NULL,
                        guest_email  TEXT NOT NULL,
                        guest_cpf    TEXT NOT NULL,
                        check_in     DATE NOT NULL,
                        check_out    DATE NOT NULL,
                        guests       INTEGER NOT NULL,
                        rate_plan    VARCHAR(20) NOT NULL,
                        subtotal     NUMERIC(12,2) NOT NULL,
                        discount     NUMERIC(12,2) NOT NULL,
                        total        NUMERIC(12,2) NOT NULL,
                        status       VARCHAR(20) NOT NULL,
                        amount_paid  NUMERIC(12,2) NOT NULL,
                        refund       NUMERIC(12,2)
                    )""");
            s.execute("""
                    CREATE TABLE IF NOT EXISTS messages (
                        id       BIGSERIAL PRIMARY KEY,
                        channel  VARCHAR(20) NOT NULL,
                        recipient TEXT NOT NULL,
                        subject  TEXT NOT NULL,
                        body     TEXT NOT NULL
                    )""");
            ROOMS.forEach((number, type) -> {
                try {
                    s.execute("INSERT INTO rooms (number, type) VALUES (" + number + ", '" + type.name()
                            + "') ON CONFLICT (number) DO NOTHING");
                } catch (SQLException e) {
                    throw new IllegalStateException(e);
                }
            });
            c.commit();
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao preparar o banco de dados", e);
        }
    }
}
