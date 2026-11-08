package pousada.db;

import java.util.List;

import pousada.domain.Room;
import pousada.domain.RoomType;

import java.math.BigDecimal;

/** Cria o que o sistema precisa num banco vazio e cadastra os quartos da pousada. */
public final class Schema {
    private static final List<String> TABLES = List.of(
            """
            CREATE TABLE IF NOT EXISTS rooms (
                number INTEGER PRIMARY KEY,
                type VARCHAR(20) NOT NULL,
                capacity INTEGER NOT NULL,
                nightly_rate NUMERIC(12,2) NOT NULL
            )""",
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
                refund NUMERIC(12,2)
            )""",
            "CREATE INDEX IF NOT EXISTS reservations_room_idx ON reservations (room_number, check_in)",
            "CREATE INDEX IF NOT EXISTS reservations_check_in_idx ON reservations (check_in)",
            """
            CREATE TABLE IF NOT EXISTS messages (
                id BIGSERIAL PRIMARY KEY,
                channel VARCHAR(20) NOT NULL,
                recipient VARCHAR(200) NOT NULL,
                subject VARCHAR(300) NOT NULL,
                body TEXT NOT NULL
            )""");

    private static final List<Room> ROOMS = List.of(
            new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90")),
            new Room(102, RoomType.STANDARD, 2, new BigDecimal("249.90")),
            new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90")),
            new Room(202, RoomType.DELUXE, 3, new BigDecimal("389.90")),
            new Room(301, RoomType.SUITE, 4, new BigDecimal("579.90")));

    private Schema() {
    }

    public static void create(Database db) {
        db.transaction(() -> db.use(connection -> {
            // Evita que duas instâncias subindo juntas criem as tabelas ao mesmo tempo.
            try (var lock = connection.createStatement()) {
                lock.execute("SELECT pg_advisory_xact_lock(7070)");
            }
            for (String ddl : TABLES) {
                try (var statement = connection.createStatement()) {
                    statement.execute(ddl);
                }
            }
            for (Room room : ROOMS) {
                try (var insert = connection.prepareStatement(
                        "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (?, ?, ?, ?) ON CONFLICT (number) DO NOTHING")) {
                    insert.setInt(1, room.number());
                    insert.setString(2, room.type().name());
                    insert.setInt(3, room.capacity());
                    insert.setBigDecimal(4, room.nightlyRate());
                    insert.executeUpdate();
                }
            }
            return null;
        }));
    }
}
