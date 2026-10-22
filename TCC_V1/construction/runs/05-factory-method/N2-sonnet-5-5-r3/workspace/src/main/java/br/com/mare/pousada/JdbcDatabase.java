package br.com.mare.pousada;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Conexão com o PostgreSQL e criação do que o sistema precisa. */
public class JdbcDatabase {
    private final String url;
    private final String user;
    private final String password;

    public JdbcDatabase(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /** Cria as tabelas; tenta de novo por um tempo caso o banco ainda esteja subindo. */
    public void createSchema() {
        SQLException last = null;
        for (int attempt = 0; attempt < 30; attempt++) {
            try (Connection c = connect(); Statement s = c.createStatement()) {
                s.execute("""
                        CREATE TABLE IF NOT EXISTS reservations (
                            id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            room_number INTEGER NOT NULL,
                            guest_name TEXT NOT NULL,
                            guest_email TEXT NOT NULL,
                            identity_kind TEXT NOT NULL,
                            identity_data TEXT NOT NULL,
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
                            id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
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
        throw new IllegalStateException("Não foi possível preparar o banco de dados", last);
    }
}
