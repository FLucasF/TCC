package br.com.marealta;

import br.com.marealta.store.PostgresStore;
import br.com.marealta.store.Store;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Mesmos testes contra um PostgreSQL real, num schema descartável. Roda só se DB_URL existir. */
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class PostgresApiTest extends ApiTestBase {
    private PostgresStore store;
    private String schema;

    @Override
    Store newStore() {
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");
        schema = "test_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection c = DriverManager.getConnection(url, user, password); Statement st = c.createStatement()) {
            st.execute("CREATE SCHEMA " + schema);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        String scoped = url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema;
        store = new PostgresStore(scoped, user, password, Config.ROOMS);
        return store;
    }

    @Override
    void closeStore() {
        store.close();
        try (Connection c = DriverManager.getConnection(System.getenv("DB_URL"), System.getenv("DB_USER"),
                System.getenv("DB_PASSWORD")); Statement st = c.createStatement()) {
            st.execute("DROP SCHEMA " + schema + " CASCADE");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
