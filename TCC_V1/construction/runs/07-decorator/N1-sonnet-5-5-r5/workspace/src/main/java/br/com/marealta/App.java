package br.com.marealta;

import br.com.marealta.db.Database;
import br.com.marealta.message.MessageDispatcher;
import br.com.marealta.message.MessagePipeline;
import br.com.marealta.message.MessageRepository;
import br.com.marealta.message.RuleCatalog;
import br.com.marealta.reservation.ReportService;
import br.com.marealta.reservation.ReservationRepository;
import br.com.marealta.reservation.ReservationService;
import br.com.marealta.reservation.RoomRepository;
import br.com.marealta.web.Routes;
import io.javalin.Javalin;

public class App {

    public static void main(String[] args) {
        AppConfig config = AppConfig.fromEnv(System.getenv());
        Database database = new Database(config.dbUrl(), config.dbUser(), config.dbPassword(), null);
        start(config, database).start(config.port());
    }

    /** Monta o sistema sobre um banco já conectado; usado também pelos testes. */
    public static Javalin start(AppConfig config, Database database) {
        database.createSchema();
        RoomRepository rooms = new RoomRepository();
        ReservationRepository reservations = new ReservationRepository();
        MessageRepository messages = new MessageRepository();
        database.run(connection -> {
            rooms.seed(connection);
            return null;
        });

        MessagePipeline pipeline = new MessagePipeline(RuleCatalog.parse(config.messageRules()), config.testMode());
        MessageDispatcher dispatcher = new MessageDispatcher(pipeline, messages);

        Javalin app = Javalin.create(javalinConfig -> javalinConfig.showJavalinBanner = false);
        new Routes(new ReservationService(database, rooms, reservations, dispatcher),
                new ReportService(database, rooms, reservations), messages, database).register(app);
        return app;
    }
}
