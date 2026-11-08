package pousada;

import pousada.domain.*;
import pousada.persistence.Database;
import pousada.persistence.MessageRepository;
import pousada.persistence.ReservationRepository;
import pousada.persistence.RoomRepository;
import pousada.service.ReportService;
import pousada.service.ReservationService;
import pousada.web.Api;

import java.util.List;
import java.util.Map;

public class Main {
    /** Todos os planos de tarifa da pousada; um plano novo entra aqui e só aqui. */
    static RatePlanCatalog ratePlans() {
        return new RatePlanCatalog(List.of(
                new StandardPlan(),
                new NonRefundablePlan(),
                new CorporatePlan(Map.of(
                        "TECNOSUL", Money.percent(12),
                        "AGROVALE", Money.percent(18),
                        "FARMAVIDA", Money.percent(25))),
                new WeekendPackagePlan(Map.of(
                        RoomType.STANDARD, Money.of("520.00"),
                        RoomType.DELUXE, Money.of("820.00"),
                        RoomType.SUITE, Money.of("1200.00"))),
                new LongStayPlan()));
    }

    public static void main(String[] args) {
        Database database = new Database(System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        database.initialize();
        RoomRepository rooms = new RoomRepository();
        ReservationRepository reservations = new ReservationRepository();
        ReservationService service = new ReservationService(database, rooms, reservations,
                new MessageRepository(), ratePlans());
        ReportService reports = new ReportService(database, rooms, reservations);
        String port = System.getenv("PORT");
        new Api(service, reports).build().start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port));
    }
}
