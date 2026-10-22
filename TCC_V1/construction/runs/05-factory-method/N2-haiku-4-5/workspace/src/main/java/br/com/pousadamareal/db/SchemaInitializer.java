package br.com.pousadamareal.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SchemaInitializer {
    public static void initialize() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS rooms (
                        number INTEGER PRIMARY KEY,
                        type VARCHAR(50) NOT NULL
                    )
                    """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS reservations (
                        id SERIAL PRIMARY KEY,
                        room_number INTEGER NOT NULL,
                        guest_name VARCHAR(255) NOT NULL,
                        guest_email VARCHAR(255) NOT NULL,
                        guest_cpf VARCHAR(14) NULL,
                        company_cnpj VARCHAR(18) NULL,
                        company_name VARCHAR(255) NULL,
                        guest_passport VARCHAR(50) NULL,
                        guest_country VARCHAR(100) NULL,
                        check_in DATE NOT NULL,
                        check_out DATE NOT NULL,
                        guests INTEGER NOT NULL,
                        rate_plan VARCHAR(50) NOT NULL,
                        nights INTEGER NOT NULL,
                        subtotal NUMERIC(10, 2) NOT NULL,
                        discount NUMERIC(10, 2) NOT NULL,
                        total NUMERIC(10, 2) NOT NULL,
                        status VARCHAR(50) NOT NULL,
                        amount_paid NUMERIC(10, 2) NOT NULL DEFAULT 0,
                        refund NUMERIC(10, 2) NULL,
                        FOREIGN KEY (room_number) REFERENCES rooms(number)
                    )
                    """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS messages (
                        id SERIAL PRIMARY KEY,
                        channel VARCHAR(50) NOT NULL,
                        recipient VARCHAR(255) NOT NULL,
                        subject VARCHAR(255) NOT NULL,
                        body TEXT NOT NULL
                    )
                    """);

                insertRooms();
            }
        }
    }

    private static void insertRooms() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO rooms (number, type) VALUES (101, 'STANDARD') ON CONFLICT DO NOTHING");
                stmt.execute("INSERT INTO rooms (number, type) VALUES (102, 'STANDARD') ON CONFLICT DO NOTHING");
                stmt.execute("INSERT INTO rooms (number, type) VALUES (201, 'DELUXE') ON CONFLICT DO NOTHING");
                stmt.execute("INSERT INTO rooms (number, type) VALUES (202, 'DELUXE') ON CONFLICT DO NOTHING");
                stmt.execute("INSERT INTO rooms (number, type) VALUES (301, 'SUITE') ON CONFLICT DO NOTHING");
            }
        }
    }
}
