package pousada;

import java.math.BigDecimal;
import pousada.db.Database;
import pousada.db.MessageRepository;
import pousada.db.ReservationRepository;
import pousada.domain.Money;
import pousada.domain.RoomCatalog;
import pousada.report.OccupancyReport;
import pousada.report.RatePlanReport;
import pousada.report.ReportRegistry;
import pousada.report.RevenueReport;
import pousada.report.TourismTaxReport;
import pousada.service.ConfirmationNotifier;
import pousada.service.ReservationService;
import pousada.web.Api;

public class Main {
    /** Taxa de turismo da prefeitura, por hóspede por noite; muda todo ano. */
    static final BigDecimal TOURISM_TAX = Money.of("4.50");

    public static void main(String[] args) {
        Database db = new Database(System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        db.createSchemaWithRetry(30);
        String port = System.getenv("PORT");
        build(db).start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port));
    }

    /** Monta o sistema: aqui se escolhe quais relatórios existem. */
    public static Api build(Database db) {
        RoomCatalog rooms = new RoomCatalog();
        ReservationRepository reservations = new ReservationRepository(db);
        MessageRepository messages = new MessageRepository(db);
        ReservationService service = new ReservationService(db, rooms, reservations, new ConfirmationNotifier(messages));
        ReportRegistry reports = new ReportRegistry()
                .register(new OccupancyReport(reservations, rooms))
                .register(new RevenueReport(reservations))
                .register(new TourismTaxReport(reservations, TOURISM_TAX))
                .register(new RatePlanReport(reservations));
        return new Api(rooms, service, messages, reports);
    }
}
