package br.com.maretalta;

import br.com.maretalta.store.PostgresStore;
import br.com.maretalta.store.Store;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;

/** Repete os testes da API contra um PostgreSQL de verdade, num schema descartável. */
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = "jdbc:postgresql:.*")
class PostgresApiTest extends ApiTest {
    private final String schema = "test_" + UUID.randomUUID().toString().replace("-", "");
    private PostgresStore pg;

    @Override
    protected Store newStore() {
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String pass = System.getenv("DB_PASSWORD");
        try (Connection c = DriverManager.getConnection(url, user, pass); Statement st = c.createStatement()) {
            st.execute("CREATE SCHEMA " + schema);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        String sep = url.contains("?") ? "&" : "?";
        pg = new PostgresStore(url + sep + "currentSchema=" + schema, user, pass);
        pg.initSchema(10_000);
        return pg;
    }

    @AfterEach
    void drop() throws Exception {
        pg.close();
        try (Connection c = DriverManager.getConnection(System.getenv("DB_URL"), System.getenv("DB_USER"),
                System.getenv("DB_PASSWORD")); Statement st = c.createStatement()) {
            st.execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }
}
