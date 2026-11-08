package com.pousadamarealta.infrastructure.database;

import com.pousadamarealta.domain.model.Room;
import com.pousadamarealta.domain.model.RoomType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initialize(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            createTables(conn);
            insertRooms(conn);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao inicializar banco de dados", e);
        }
    }

    private static void createTables(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rooms (
                    number VARCHAR(10) PRIMARY KEY,
                    type VARCHAR(20) NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                    id SERIAL PRIMARY KEY,
                    room_number VARCHAR(10) NOT NULL REFERENCES rooms(number),
                    guest_name VARCHAR(255) NOT NULL,
                    guest_email VARCHAR(255) NOT NULL,
                    guest_cpf VARCHAR(11) NOT NULL,
                    check_in DATE NOT NULL,
                    check_out DATE NOT NULL,
                    guests INTEGER NOT NULL,
                    rate_plan VARCHAR(50) NOT NULL,
                    company_code VARCHAR(50),
                    nights INTEGER NOT NULL,
                    subtotal DECIMAL(10, 2) NOT NULL,
                    discount DECIMAL(10, 2) NOT NULL,
                    total DECIMAL(10, 2) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    amount_paid DECIMAL(10, 2) NOT NULL,
                    refund DECIMAL(10, 2)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id SERIAL PRIMARY KEY,
                    channel VARCHAR(20) NOT NULL,
                    recipient VARCHAR(255) NOT NULL,
                    subject VARCHAR(500) NOT NULL,
                    body TEXT NOT NULL
                )
            """);
        }
    }

    private static void insertRooms(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM rooms");
            stmt.execute("INSERT INTO rooms (number, type) VALUES ('101', 'STANDARD')");
            stmt.execute("INSERT INTO rooms (number, type) VALUES ('102', 'STANDARD')");
            stmt.execute("INSERT INTO rooms (number, type) VALUES ('201', 'DELUXE')");
            stmt.execute("INSERT INTO rooms (number, type) VALUES ('202', 'DELUXE')");
            stmt.execute("INSERT INTO rooms (number, type) VALUES ('301', 'SUITE')");
        }
    }
}
