package br.com.marealta;

import br.com.marealta.persistence.Database;
import br.com.marealta.persistence.MessageRepository;
import br.com.marealta.persistence.ReservationRepository;
import br.com.marealta.service.ReportService;
import br.com.marealta.service.ReservationService;
import br.com.marealta.web.Api;

public class App {
    public static void main(String[] args) {
        Database database = new Database(System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        database.createSchema();
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
        start(database, port);
    }

    public static io.javalin.Javalin start(Database database, int port) {
        ReservationRepository reservations = new ReservationRepository();
        ReservationService service = new ReservationService(database, reservations, new MessageRepository());
        return new Api(service, new ReportService(database, reservations)).start(port);
    }
}
