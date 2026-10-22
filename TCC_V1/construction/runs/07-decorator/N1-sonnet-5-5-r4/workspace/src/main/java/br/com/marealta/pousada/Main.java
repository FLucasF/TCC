package br.com.marealta.pousada;

import br.com.marealta.pousada.messaging.MessagePipeline;
import br.com.marealta.pousada.messaging.MessageRules;
import br.com.marealta.pousada.persistence.Database;
import br.com.marealta.pousada.service.ReportService;
import br.com.marealta.pousada.service.ReservationService;
import br.com.marealta.pousada.web.App;
import io.javalin.Javalin;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        Config config = Config.fromEnvironment(System.getenv());
        start(config).start(config.port());
    }

    /** Monta o sistema todo, ainda sem ligar o servidor. */
    public static Javalin start(Config config) {
        Database database = new Database(config.dbUrl(), config.dbUser(), config.dbPassword());
        MessagePipeline pipeline = new MessagePipeline(MessageRules.parse(config.messageRules()), config.testMode());
        return new App(new ReservationService(database, pipeline), new ReportService(database)).create(database::close);
    }
}
