package pousada;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

class Database {
    interface Work<T> {
        T run(Connection connection) throws SQLException;
    }

    private final String url;
    private final String user;
    private final String password;

    Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    <T> T transaction(Work<T> work) {
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (Throwable e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Cria o que falta no banco e espera por ele se ainda estiver subindo. */
    void initialize() {
        SQLException last = null;
        for (int attempt = 0; attempt < 30; attempt++) {
            try (Connection c = connect(); Statement s = c.createStatement()) {
                c.setAutoCommit(false);
                s.execute("select pg_advisory_xact_lock(7070)");
                s.execute("""
                        create table if not exists rooms (
                          number int primary key,
                          type text not null,
                          capacity int not null,
                          nightly_rate numeric(12,2) not null)""");
                s.execute("""
                        create table if not exists reservations (
                          id bigserial primary key,
                          room_number int not null references rooms(number),
                          guest_name text not null,
                          guest_email text not null,
                          id_type text not null,
                          id_values text not null,
                          check_in date not null,
                          check_out date not null,
                          guests int not null,
                          rate_plan text not null,
                          nights int not null,
                          subtotal numeric(12,2) not null,
                          discount numeric(12,2) not null,
                          total numeric(12,2) not null,
                          status text not null,
                          amount_paid numeric(12,2) not null default 0,
                          refund numeric(12,2))""");
                s.execute("""
                        create table if not exists messages (
                          id bigserial primary key,
                          channel text not null,
                          recipient text not null,
                          subject text not null,
                          body text not null)""");
                s.execute("""
                        insert into rooms (number, type, capacity, nightly_rate) values
                          (101, 'STANDARD', 2, 249.90), (102, 'STANDARD', 2, 249.90),
                          (201, 'DELUXE', 3, 389.90), (202, 'DELUXE', 3, 389.90),
                          (301, 'SUITE', 4, 579.90)
                        on conflict (number) do nothing""");
                c.commit();
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
        throw new IllegalStateException("Banco indisponível", last);
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
