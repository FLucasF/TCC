package br.com.marealta.persistence;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

/** Cria o que o sistema precisa no banco, se ainda não existir. */
public final class Schema {
    private static final List<String> STATEMENTS = List.of(
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
                refund NUMERIC(12,2)
            )""",
            """
            CREATE TABLE IF NOT EXISTS messages (
                id BIGSERIAL PRIMARY KEY,
                channel VARCHAR(20) NOT NULL,
                to_address TEXT NOT NULL,
                subject TEXT NOT NULL,
                body TEXT NOT NULL,
                status VARCHAR(20) NOT NULL
            )""");

    private static final String INSERT_ROOM =
            "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (?, ?, ?, ?::numeric) "
                    + "ON CONFLICT (number) DO NOTHING";

    private static final String[][] ROOMS = {
            {"101", "STANDARD", "2", "249.90"},
            {"102", "STANDARD", "2", "249.90"},
            {"201", "DELUXE", "3", "389.90"},
            {"202", "DELUXE", "3", "389.90"},
            {"301", "SUITE", "4", "579.90"},
    };

    private Schema() {
    }

    public static void create(Db db) {
        db.transaction(connection -> {
            try (Statement statement = connection.createStatement()) {
                for (String sql : STATEMENTS) {
                    statement.execute(sql);
                }
            }
            try (PreparedStatement insert = connection.prepareStatement(INSERT_ROOM)) {
                for (String[] room : ROOMS) {
                    insert.setInt(1, Integer.parseInt(room[0]));
                    insert.setString(2, room[1]);
                    insert.setInt(3, Integer.parseInt(room[2]));
                    insert.setString(4, room[3]);
                    insert.executeUpdate();
                }
            }
            return null;
        });
    }
}
