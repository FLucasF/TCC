package br.com.mealta.store;

import java.sql.Statement;

public final class Schema {
    private Schema() {
    }

    public static void create(Database db) {
        db.withConnection(connection -> {
            try (Statement st = connection.createStatement()) {
                st.execute("""
                        CREATE TABLE IF NOT EXISTS reservations (
                            id BIGSERIAL PRIMARY KEY,
                            room_number INT NOT NULL,
                            guest_name TEXT NOT NULL,
                            guest_email TEXT NOT NULL,
                            guest_cpf TEXT NOT NULL,
                            check_in DATE NOT NULL,
                            check_out DATE NOT NULL,
                            guests INT NOT NULL,
                            rate_plan TEXT NOT NULL,
                            subtotal NUMERIC(12,2) NOT NULL,
                            discount NUMERIC(12,2) NOT NULL,
                            total NUMERIC(12,2) NOT NULL,
                            status TEXT NOT NULL,
                            amount_paid NUMERIC(12,2) NOT NULL,
                            refund NUMERIC(12,2)
                        )""");
                st.execute("CREATE INDEX IF NOT EXISTS reservations_room_idx ON reservations (room_number)");
                st.execute("CREATE INDEX IF NOT EXISTS reservations_check_in_idx ON reservations (check_in)");
                st.execute("""
                        CREATE TABLE IF NOT EXISTS messages (
                            id BIGSERIAL PRIMARY KEY,
                            channel TEXT NOT NULL,
                            recipient TEXT NOT NULL,
                            subject TEXT NOT NULL,
                            body TEXT NOT NULL
                        )""");
            }
            return null;
        });
    }
}
