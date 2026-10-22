package br.com.marealta;

import br.com.marealta.mail.MailPipeline;
import br.com.marealta.mail.MailRules;
import br.com.marealta.mail.Mailer;
import br.com.marealta.persistence.Db;
import br.com.marealta.persistence.MessageRepository;
import br.com.marealta.persistence.ReservationRepository;
import br.com.marealta.persistence.RoomRepository;
import br.com.marealta.persistence.Schema;
import br.com.marealta.report.OccupancyReport;
import br.com.marealta.report.RevenueReport;
import br.com.marealta.web.Routes;
import io.javalin.Javalin;

public final class App {
    private App() {
    }

    public static void main(String[] args) throws InterruptedException {
        Settings settings = Settings.fromEnvironment(System.getenv());
        Db db = new Db(settings.dbUrl(), settings.dbUser(), settings.dbPassword());
        createSchemaWhenDatabaseIsUp(db);

        RoomRepository rooms = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messages = new MessageRepository();
        Mailer mailer = new Mailer(new MailPipeline(settings.testMode(), MailRules.forThisInstallation()), messages);
        ReservationService service = new ReservationService(db, rooms, reservationRepository, mailer);

        Routes routes = new Routes(db, rooms, messages, service,
                new OccupancyReport(db, rooms, reservationRepository),
                new RevenueReport(db, reservationRepository));
        Javalin app = Javalin.create(config -> config.showJavalinBanner = false);
        routes.register(app);
        app.start(settings.port());
    }

    private static void createSchemaWhenDatabaseIsUp(Db db) throws InterruptedException {
        for (int attempt = 1; ; attempt++) {
            try {
                Schema.create(db);
                return;
            } catch (RuntimeException e) {
                if (attempt >= 30) {
                    throw e;
                }
                Thread.sleep(1000);
            }
        }
    }
}
