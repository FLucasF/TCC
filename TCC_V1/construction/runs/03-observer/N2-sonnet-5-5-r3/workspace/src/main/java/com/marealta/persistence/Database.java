package com.marealta.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Supplier;

public final class Database {
    private final String url;
    private final String user;
    private final String password;
    private final ThreadLocal<Connection> current = new ThreadLocal<>();

    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /** Cria o que o sistema precisa; espera o banco aceitar conexões, que pode demorar a subir. */
    public void migrate() {
        SQLException last = null;
        for (int attempt = 0; attempt < 30; attempt++) {
            try {
                createSchema();
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

    private void createSchema() throws SQLException {
        try (Connection c = open(); Statement s = c.createStatement()) {
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
            s.execute("""
                    CREATE TABLE IF NOT EXISTS messages (
                        id BIGSERIAL PRIMARY KEY,
                        channel TEXT NOT NULL,
                        recipient TEXT NOT NULL,
                        subject TEXT NOT NULL,
                        body TEXT NOT NULL
                    )""");
        }
    }

    private Connection open() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /** Executa o trabalho numa única transação; os repositórios usam a conexão dela. */
    public <T> T transaction(Supplier<T> work) {
        if (current.get() != null) {
            return work.get();
        }
        try (Connection c = open()) {
            c.setAutoCommit(false);
            current.set(c);
            try {
                T result = work.get();
                c.commit();
                return result;
            } catch (RuntimeException | Error e) {
                c.rollback();
                throw e;
            } finally {
                current.remove();
            }
        } catch (SQLException e) {
            throw new UncheckedSqlException(e);
        }
    }

    Connection connection() {
        Connection c = current.get();
        if (c == null) {
            throw new IllegalStateException("Operação de banco fora de uma transação");
        }
        return c;
    }
}
