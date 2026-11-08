package pousada;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/** Cria no banco o que o sistema precisa e cadastra os quartos da pousada. */
public final class Schema {
    private static final List<Room> ROOMS = List.of(
            new Room(101, RoomType.STANDARD, 2, Money.of("249.90")),
            new Room(102, RoomType.STANDARD, 2, Money.of("249.90")),
            new Room(201, RoomType.DELUXE, 3, Money.of("389.90")),
            new Room(202, RoomType.DELUXE, 3, Money.of("389.90")),
            new Room(301, RoomType.SUITE, 4, Money.of("579.90")));

    private static final List<String> DDL = List.of(
            """
            CREATE TABLE IF NOT EXISTS rooms (
              number INTEGER PRIMARY KEY,
              type VARCHAR(20) NOT NULL,
              capacity INTEGER NOT NULL,
              nightly_rate NUMERIC(12,2) NOT NULL)
            """,
            """
            CREATE TABLE IF NOT EXISTS reservations (
              id BIGSERIAL PRIMARY KEY,
              room_number INTEGER NOT NULL REFERENCES rooms(number),
              guest_name VARCHAR(200) NOT NULL,
              guest_email VARCHAR(200) NOT NULL,
              guest_cpf VARCHAR(30) NOT NULL,
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
              refund NUMERIC(12,2))
            """,
            """
            CREATE TABLE IF NOT EXISTS messages (
              id BIGSERIAL PRIMARY KEY,
              channel VARCHAR(20) NOT NULL,
              recipient VARCHAR(200) NOT NULL,
              subject VARCHAR(300) NOT NULL,
              body TEXT NOT NULL)
            """);

    private Schema() {
    }

    public static void init(Db db) {
        awaitDatabase(db);
        db.tx(c -> {
            try (Statement lock = c.createStatement()) {
                lock.execute("SELECT pg_advisory_xact_lock(7070)");
            }
            for (String ddl : DDL) {
                try (Statement s = c.createStatement()) {
                    s.execute(ddl);
                }
            }
            for (Room room : ROOMS) {
                insertRoom(c, room);
            }
            return null;
        });
    }

    private static void insertRoom(Connection c, Room room) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING")) {
            ps.setInt(1, room.number());
            ps.setString(2, room.type().name());
            ps.setInt(3, room.capacity());
            ps.setBigDecimal(4, room.nightlyRate());
            ps.executeUpdate();
        }
    }

    private static void awaitDatabase(Db db) {
        SQLException last = null;
        for (int attempt = 0; attempt < 60; attempt++) {
            try (Connection ignored = db.open()) {
                return;
            } catch (SQLException e) {
                last = e;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw new IllegalStateException("Banco de dados indisponível", last);
    }
}
