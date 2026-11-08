package br.com.pousadamarealta.reservas.persistence;

import br.com.pousadamarealta.reservas.domain.Room;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/** Cria no banco vazio o que o sistema precisa. Pode rodar quantas vezes for preciso. */
public final class Schema {
    private Schema() {}

    public static void create(Database db) {
        db.transaction(() -> {
            try (Statement st = db.connection().createStatement()) {
                // evita corrida entre duas instâncias subindo juntas
                st.execute("SELECT pg_advisory_xact_lock(748391)");
                st.execute("""
                        CREATE TABLE IF NOT EXISTS rooms (
                            number INTEGER PRIMARY KEY,
                            type VARCHAR(20) NOT NULL,
                            capacity INTEGER NOT NULL,
                            nightly_rate NUMERIC(12,2) NOT NULL
                        )""");
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
                            refund NUMERIC(12,2),
                            CHECK (check_out > check_in)
                        )""");
                st.execute("CREATE INDEX IF NOT EXISTS reservations_room_dates ON reservations (room_number, check_in)");
                st.execute("""
                        CREATE TABLE IF NOT EXISTS messages (
                            id BIGSERIAL PRIMARY KEY,
                            channel VARCHAR(20) NOT NULL,
                            recipient TEXT NOT NULL,
                            subject TEXT NOT NULL,
                            body TEXT NOT NULL
                        )""");
                try (PreparedStatement ps = db.connection().prepareStatement(
                        "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (?, ?, ?, ?) ON CONFLICT (number) DO NOTHING")) {
                    for (Room room : Room.DEFAULTS) {
                        ps.setInt(1, room.number());
                        ps.setString(2, room.type().name());
                        ps.setInt(3, room.capacity());
                        ps.setBigDecimal(4, room.nightlyRate());
                        ps.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                throw new DataAccessException(e);
            }
            return null;
        });
    }
}
