package br.com.mare;

import br.com.mare.domain.ReportService;
import br.com.mare.domain.ReservationService;
import br.com.mare.infra.Db;
import br.com.mare.infra.MessageRepository;
import br.com.mare.infra.ReservationRepository;
import br.com.mare.web.Routes;
import io.javalin.Javalin;

public class App {
    public static void main(String[] args) {
        Db db = new Db(System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        db.migrate();
        ReservationRepository reservations = new ReservationRepository();
        ReservationService service = new ReservationService(db, reservations, new MessageRepository());
        ReportService reports = new ReportService(db, reservations);

        String port = System.getenv("PORT");
        Routes.register(Javalin.create(), service, reports).start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port));
    }
}
