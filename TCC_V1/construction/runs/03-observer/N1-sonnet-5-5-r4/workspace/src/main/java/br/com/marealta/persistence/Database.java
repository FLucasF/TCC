package br.com.marealta.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {
    private Database() {
    }

    /** Abre a conexão (esperando o banco subir, se preciso) e cria o que faltar. */
    public static HikariDataSource open(String url, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setInitializationFailTimeout(60_000);
        HikariDataSource dataSource = new HikariDataSource(config);
        createSchema(dataSource);
        return dataSource;
    }

    static void createSchema(javax.sql.DataSource dataSource) {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.execute("""
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
                        nights INT NOT NULL,
                        subtotal NUMERIC(12,2) NOT NULL,
                        discount NUMERIC(12,2) NOT NULL,
                        total NUMERIC(12,2) NOT NULL,
                        status TEXT NOT NULL,
                        amount_paid NUMERIC(12,2) NOT NULL,
                        refund NUMERIC(12,2)
                    )""");
            s.execute("CREATE INDEX IF NOT EXISTS reservations_room_idx ON reservations (room_number)");
            s.execute("""
                    CREATE TABLE IF NOT EXISTS messages (
                        id BIGSERIAL PRIMARY KEY,
                        channel TEXT NOT NULL,
                        recipient TEXT NOT NULL,
                        subject TEXT NOT NULL,
                        body TEXT NOT NULL
                    )""");
        } catch (SQLException e) {
            throw new IllegalStateException("Não foi possível preparar o banco", e);
        }
    }
}
