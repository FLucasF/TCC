package br.com.marealta.pousada.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    public interface Work<T> {
        T run(Connection connection) throws SQLException;
    }

    private final String url;
    private final String user;
    private final String password;

    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /** Cria o que o sistema precisa; espera o banco aceitar conexões se ele ainda estiver subindo. */
    public void initialize() {
        SQLException last = null;
        for (int attempt = 1; attempt <= 30; attempt++) {
            try {
                inTransaction(c -> {
                    try (Statement st = c.createStatement()) {
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
                                    nights INT NOT NULL,
                                    subtotal NUMERIC(12,2) NOT NULL,
                                    discount NUMERIC(12,2) NOT NULL,
                                    total NUMERIC(12,2) NOT NULL,
                                    status TEXT NOT NULL,
                                    amount_paid NUMERIC(12,2) NOT NULL,
                                    refund NUMERIC(12,2)
                                )""");
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

    public <T> T inTransaction(Work<T> work) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (Throwable t) {
                connection.rollback();
                throw t;
            }
        }
    }
}
