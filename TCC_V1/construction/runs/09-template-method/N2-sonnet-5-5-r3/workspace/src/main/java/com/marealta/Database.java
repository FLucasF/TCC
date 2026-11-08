package com.marealta;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {
    private Database() {}

    /** Cria o que o sistema precisa num banco vazio; não mexe no que já existe. */
    public static void createSchema(DataSource dataSource) {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.execute("""
                    CREATE TABLE IF NOT EXISTS reservations (
                        id BIGSERIAL PRIMARY KEY,
                        room_number INT NOT NULL,
                        guest_name VARCHAR(200) NOT NULL,
                        guest_email VARCHAR(200) NOT NULL,
                        guest_cpf VARCHAR(30) NOT NULL,
                        check_in DATE NOT NULL,
                        check_out DATE NOT NULL,
                        guests INT NOT NULL,
                        rate_plan VARCHAR(30) NOT NULL,
                        nights INT NOT NULL,
                        subtotal NUMERIC(12,2) NOT NULL,
                        discount NUMERIC(12,2) NOT NULL,
                        total NUMERIC(12,2) NOT NULL,
                        status VARCHAR(30) NOT NULL,
                        amount_paid NUMERIC(12,2) NOT NULL,
                        refund NUMERIC(12,2)
                    )""");
            s.execute("""
                    CREATE TABLE IF NOT EXISTS messages (
                        id BIGSERIAL PRIMARY KEY,
                        channel VARCHAR(20) NOT NULL,
                        recipient VARCHAR(200) NOT NULL,
                        subject VARCHAR(300) NOT NULL,
                        body TEXT NOT NULL
                    )""");
        } catch (SQLException e) {
            throw new IllegalStateException("Não foi possível criar o esquema do banco", e);
        }
    }
}
