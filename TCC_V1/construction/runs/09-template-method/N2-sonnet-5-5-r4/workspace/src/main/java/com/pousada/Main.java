package com.pousada;

import com.pousada.domain.RoomCatalog;
import com.pousada.report.OccupancyReport;
import com.pousada.report.RatePlanReport;
import com.pousada.report.Report;
import com.pousada.report.ReportRegistry;
import com.pousada.report.RevenueReport;
import com.pousada.report.TourismTaxReport;
import com.pousada.repository.Database;
import com.pousada.repository.MessageRepository;
import com.pousada.repository.PostgresMessageRepository;
import com.pousada.repository.PostgresReservationRepository;
import com.pousada.repository.ReservationRepository;
import com.pousada.service.ReservationService;
import com.pousada.web.Api;
import io.javalin.Javalin;
import java.math.BigDecimal;
import java.util.List;

public class Main {
    /** Taxa de turismo da prefeitura, por hóspede por noite. Muda todo ano. */
    static final BigDecimal TOURISM_TAX_PER_GUEST_NIGHT = new BigDecimal("4.50");

    public static void main(String[] args) {
        Database database = Database.connect(System.getenv("DB_URL"), System.getenv("DB_USER"),
                System.getenv("DB_PASSWORD"));
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
        build(new PostgresReservationRepository(database), new PostgresMessageRepository(database)).start(port);
    }

    public static Javalin build(ReservationRepository reservations, MessageRepository messages) {
        RoomCatalog rooms = new RoomCatalog();
        ReservationService service = new ReservationService(rooms, reservations, messages);
        List<Report<?>> available = List.of(
                new OccupancyReport(reservations, rooms),
                new RevenueReport(reservations),
                new TourismTaxReport(reservations, TOURISM_TAX_PER_GUEST_NIGHT),
                new RatePlanReport(reservations));
        return new Api(rooms, service, messages, new ReportRegistry(available)).create();
    }
}
