package pousada;

import pousada.repo.Database;
import pousada.repo.JdbcMessageRepository;
import pousada.repo.JdbcReservationRepository;
import pousada.reports.OccupancyReport;
import pousada.reports.RatePlanReport;
import pousada.reports.RevenueReport;
import pousada.reports.TourismTaxReport;
import pousada.service.ConfirmationMailer;
import pousada.service.ReservationService;
import pousada.web.Api;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;

public final class Main {
    /** Taxa de turismo por hóspede por noite; a prefeitura muda todo ano. */
    private static final BigDecimal TOURISM_TAX = new BigDecimal("4.50");

    public static void main(String[] args) {
        DataSource ds = Database.connect(required("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        var reservations = new JdbcReservationRepository(ds);
        var messages = new JdbcMessageRepository(ds);
        var service = new ReservationService(reservations, new ConfirmationMailer(messages));
        var reports = List.<pousada.reports.Report<?>>of(
                new OccupancyReport(reservations),
                new RevenueReport(reservations),
                new TourismTaxReport(reservations, TOURISM_TAX),
                new RatePlanReport(reservations));
        String port = System.getenv("PORT");
        Api.create(service, messages, reports).start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port));
    }

    private static String required(String name) {
        String v = System.getenv(name);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Variável de ambiente ausente: " + name);
        }
        return v;
    }
}
