package br.com.marealta.pousada.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    private final String url;
    private final String user;
    private final String password;

    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public Connection connection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /** Cria o que falta no banco; espera o banco subir se ele ainda não atende. */
    public void migrate() {
        SQLException last = null;
        for (int attempt = 0; attempt < 30; attempt++) {
            try (Connection c = connection(); Statement s = c.createStatement()) {
                s.execute("""
                        CREATE TABLE IF NOT EXISTS reservations (
                            id BIGSERIAL PRIMARY KEY,
                            room_number INTEGER NOT NULL,
                            guest_name TEXT NOT NULL,
                            guest_email TEXT NOT NULL,
                            guest_cpf TEXT NOT NULL,
                            check_in DATE NOT NULL,
                            check_out DATE NOT NULL,
                            guests INTEGER NOT NULL,
                            rate_plan TEXT NOT NULL,
                            nights INTEGER NOT NULL,
                            subtotal NUMERIC(12,2) NOT NULL,
                            discount NUMERIC(12,2) NOT NULL,
                            total NUMERIC(12,2) NOT NULL,
                            status TEXT NOT NULL,
                            amount_paid NUMERIC(12,2) NOT NULL,
                            refund NUMERIC(12,2)
                        )""");
                s.execute("""
                        CREATE TABLE IF NOT EXISTS messages (
                            id BIGSERIAL PRIMARY KEY,
                            channel TEXT NOT NULL,
                            recipient TEXT NOT NULL,
                            subject TEXT NOT NULL,
                            body TEXT NOT NULL
                        )""");
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
        throw new IllegalStateException("não foi possível preparar o banco", last);
    }
}
