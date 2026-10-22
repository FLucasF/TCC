package br.com.marealta.pousada;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Function;

public class Db implements AutoCloseable {
    private static final long WRITE_LOCK = 7001L;

    private final HikariDataSource ds;

    public Db(String url, String user, String password) {
        HikariConfig c = new HikariConfig();
        c.setJdbcUrl(url);
        if (user != null) c.setUsername(user);
        if (password != null) c.setPassword(password);
        c.setMaximumPoolSize(10);
        c.setInitializationFailTimeout(-1);
        this.ds = new HikariDataSource(c);
    }

    /** Cria o que falta no banco; tenta por um tempo, caso o banco ainda esteja subindo. */
    public void init() {
        RuntimeException last = null;
        for (int i = 0; i < 30; i++) {
            try {
                write(repo -> { repo.createSchema(); return null; });
                return;
            } catch (RuntimeException e) {
                last = e;
                try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
        }
        throw new IllegalStateException("Não foi possível preparar o banco", last);
    }

    /** Transação de escrita, serializada entre todas as instâncias (evita reservas sobrepostas). */
    public <T> T write(Function<Repo, T> work) {
        return run(true, work);
    }

    public <T> T read(Function<Repo, T> work) {
        return run(false, work);
    }

    private <T> T run(boolean lock, Function<Repo, T> work) {
        try (Connection conn = ds.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (lock) {
                    try (Statement st = conn.createStatement()) {
                        st.execute("SELECT pg_advisory_xact_lock(" + WRITE_LOCK + ")");
                    }
                }
                T result = work.apply(new Repo(conn));
                conn.commit();
                return result;
            } catch (Throwable t) {
                conn.rollback();
                throw t;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close() {
        ds.close();
    }
}
