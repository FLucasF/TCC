package br.com.marealta;

import br.com.marealta.report.Reports;
import br.com.marealta.service.ReservationService;
import br.com.marealta.store.PostgresStore;
import br.com.marealta.web.WebApp;
import io.javalin.Javalin;

public class Main {
    public static void main(String[] args) {
        Config config = Config.fromEnv();
        PostgresStore store = new PostgresStore(config.dbUrl(), config.dbUser(), config.dbPassword(), Config.ROOMS);
        Javalin app = WebApp.create(new ReservationService(store),
                Reports.all(store, config.tourismTaxPerGuestNight()));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            store.close();
        }));
        app.start(config.port());
    }
}
