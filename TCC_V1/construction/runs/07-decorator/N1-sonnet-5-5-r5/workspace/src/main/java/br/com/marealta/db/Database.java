package br.com.marealta.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Database implements AutoCloseable {

    public interface Work<T> {
        T run(Connection connection) throws SQLException;
    }

    private static final String SCHEMA_SQL = """
            create table if not exists rooms (
                number integer primary key,
                type text not null,
                capacity integer not null,
                nightly_rate numeric(12,2) not null
            );
            create table if not exists reservations (
                id bigserial primary key,
                room_number integer not null references rooms(number),
                guest_name text not null,
                guest_email text not null,
                guest_cpf text not null,
                check_in date not null,
                check_out date not null,
                guests integer not null,
                rate_plan text not null,
                nights integer not null,
                subtotal numeric(12,2) not null,
                discount numeric(12,2) not null,
                total numeric(12,2) not null,
                status text not null,
                amount_paid numeric(12,2) not null,
                refund numeric(12,2)
            );
            create index if not exists reservations_room_idx on reservations(room_number, check_in);
            create table if not exists messages (
                id bigserial primary key,
                channel text not null,
                recipient text not null,
                subject text not null,
                body text not null,
                status text not null
            );
            """;

    private final HikariDataSource dataSource;

    /** O esquema é opcional: permite isolar os dados (usado nos testes). */
    public Database(String url, String user, String password, String schema) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setConnectionTimeout(10_000);
        config.setInitializationFailTimeout(60_000);
        if (schema != null) {
            config.setConnectionInitSql("set search_path to " + schema);
        }
        this.dataSource = new HikariDataSource(config);
    }

    public void createSchema() {
        run(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("select pg_advisory_xact_lock(7070)");
                statement.execute(SCHEMA_SQL);
            }
            return null;
        });
    }

    /** Executa tudo numa transação: ou entra tudo, ou nada. */
    public <T> T run(Work<T> work) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (Throwable e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Falha no banco de dados: " + e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
