package br.com.marealta;

import br.com.marealta.http.Api;
import br.com.marealta.reports.OccupancyReport;
import br.com.marealta.reports.RatePlanReport;
import br.com.marealta.reports.Report;
import br.com.marealta.reports.RevenueReport;
import br.com.marealta.reports.TourismTaxReport;

import java.math.BigDecimal;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
        // Valor por hóspede por noite; a prefeitura muda todo ano.
        BigDecimal tourismTax = Money.of(System.getenv().getOrDefault("TOURISM_TAX_RATE", "4.50"));

        Outbox outbox = new Outbox();
        ReservationService reservations = new ReservationService(outbox);
        List<Report> reports = List.of(
                new OccupancyReport(reservations),
                new RevenueReport(reservations),
                new TourismTaxReport(reservations, tourismTax),
                new RatePlanReport(reservations));
        new Api(reservations, outbox, reports).start(port);
    }
}
