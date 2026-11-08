package br.com.marealta;

import br.com.marealta.domain.ReservationService;
import br.com.marealta.domain.TourismTax;
import br.com.marealta.report.Reports;
import br.com.marealta.store.JdbcStore;
import br.com.marealta.store.Store;
import br.com.marealta.web.Routes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public final class App {
    private App() {}

    public static ObjectMapper newMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public static Javalin create(Store store) {
        ObjectMapper mapper = newMapper();
        Javalin app = Javalin.create(config -> config.jsonMapper(new JavalinJackson(mapper, false)));
        ReservationService service = new ReservationService(store);
        Routes.register(app, mapper, service, store, Reports.all(TourismTax.standard()));
        return app;
    }

    public static void main(String[] args) {
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(requireEnv("DB_URL"));
        hikari.setUsername(System.getenv("DB_USER"));
        hikari.setPassword(System.getenv("DB_PASSWORD"));
        hikari.setMaximumPoolSize(10);
        hikari.setInitializationFailTimeout(60_000); // o banco pode demorar a subir
        HikariDataSource dataSource = new HikariDataSource(hikari);

        JdbcStore store = new JdbcStore(dataSource);
        store.initialize(Config.initialRooms());

        Javalin app = create(store).start(Config.port());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            dataSource.close();
        }));
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável de ambiente obrigatória ausente: " + name);
        }
        return value;
    }
}
