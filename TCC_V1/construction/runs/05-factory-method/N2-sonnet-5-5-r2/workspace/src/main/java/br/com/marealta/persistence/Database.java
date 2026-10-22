package br.com.marealta.persistence;

import br.com.marealta.domain.IdentityKinds;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

public final class Database {
    @FunctionalInterface
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

    public <T> T transaction(Work<T> work) {
        try (Connection c = DriverManager.getConnection(url, user, password)) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro de banco de dados", e);
        }
    }

    /** Cria o que falta no banco; espera o banco ficar disponível. */
    public void initialize() {
        long deadline = System.currentTimeMillis() + 60_000;
        while (true) {
            try {
                createSchema();
                return;
            } catch (RuntimeException e) {
                if (System.currentTimeMillis() > deadline) {
                    throw e;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }

    private void createSchema() {
        String identityColumns = IdentityKinds.allFields().stream()
                .map(f -> f + " text")
                .collect(Collectors.joining(", "));
        transaction(c -> {
            try (Statement s = c.createStatement()) {
                s.execute("select pg_advisory_xact_lock(7070)");
                s.execute("create table if not exists reservations ("
                        + "id bigserial primary key, room_number int not null, "
                        + "guest_name text not null, guest_email text not null, "
                        + identityColumns + ", "
                        + "check_in date not null, check_out date not null, guests int not null, "
                        + "rate_plan text not null, nights int not null, "
                        + "subtotal numeric(12,2) not null, discount numeric(12,2) not null, "
                        + "total numeric(12,2) not null, status text not null, "
                        + "amount_paid numeric(12,2) not null, refund numeric(12,2))");
                s.execute("create table if not exists messages ("
                        + "id bigserial primary key, channel text not null, recipient text not null, "
                        + "subject text not null, body text not null)");
            }
            return null;
        });
    }
}
