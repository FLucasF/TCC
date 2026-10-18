package shop;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import java.time.Clock;
import shop.domain.Catalog;
import shop.domain.shipping.TransLogClient;
import shop.persistence.postgres.PostgresStore;
import shop.service.Application;
import shop.web.Routes;

public final class App {
    private App() {
    }

    public static void main(String[] args) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(System.getenv("DB_URL"));
        config.setUsername(System.getenv("DB_USER"));
        config.setPassword(System.getenv("DB_PASSWORD"));
        config.setMaximumPoolSize(10);
        HikariDataSource dataSource = new HikariDataSource(config);

        PostgresStore store = new PostgresStore(dataSource);
        store.migrate();
        store.inTransaction(tx -> {
            Catalog.initial().forEach(tx.products()::insertIfAbsent);
            return null;
        });

        Application services = Application.create(store, new TransLogClient(System.getenv("TRANSLOG_URL")),
                Clock.systemUTC());
        Javalin app = Javalin.create();
        Routes.register(app, services);
        String port = System.getenv("PORT");
        app.start(port == null || port.isBlank() ? 8080 : Integer.parseInt(port));
    }
}
