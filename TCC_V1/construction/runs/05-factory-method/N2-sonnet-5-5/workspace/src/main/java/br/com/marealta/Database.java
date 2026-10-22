package br.com.marealta;

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

    public Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /** Cria o que o sistema precisa; espera o banco aceitar conexões por até ~60s. */
    public void migrate() {
        SQLException last = null;
        for (int attempt = 0; attempt < 30; attempt++) {
            try (Connection c = connect(); Statement s = c.createStatement()) {
                s.execute("""
                        CREATE TABLE IF NOT EXISTS reservations (
                          id BIGSERIAL PRIMARY KEY,
                          room_number INT NOT NULL,
                          guest_name TEXT NOT NULL,
                          guest_email TEXT NOT NULL,
                          identity_kind TEXT NOT NULL,
                          identity_data TEXT NOT NULL,
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
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw new IllegalStateException("Não foi possível preparar o banco", last);
    }
}
