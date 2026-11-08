package br.com.mare;

import br.com.mare.domain.Money;
import br.com.mare.domain.ReservationService;
import br.com.mare.persistence.Database;
import br.com.mare.persistence.JdbcMessageRepository;
import br.com.mare.persistence.JdbcReservationRepository;
import br.com.mare.report.OccupancyReport;
import br.com.mare.report.RatePlanReport;
import br.com.mare.report.Report;
import br.com.mare.report.ReservationQueries;
import br.com.mare.report.RevenueReport;
import br.com.mare.report.TourismTaxReport;
import br.com.mare.web.Api;
import io.javalin.Javalin;

import java.math.BigDecimal;
import java.util.List;

import javax.sql.DataSource;

public final class Main {
    private static final String DEFAULT_TOURISM_TAX = "4.50";

    private Main() {
    }

    public static void main(String[] args) {
        DataSource dataSource = Database.connect(
                require("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        String port = System.getenv("PORT");
        build(dataSource, Money.of(env("TOURISM_TAX", DEFAULT_TOURISM_TAX)))
                .start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }

    /** Monta a aplicação: é aqui que se registram os relatórios. */
    public static Javalin build(DataSource dataSource, BigDecimal tourismTax) {
        JdbcReservationRepository reservations = new JdbcReservationRepository(dataSource);
        JdbcMessageRepository messages = new JdbcMessageRepository(dataSource);
        ReservationQueries queries = new ReservationQueries(reservations);
        List<Report<?>> reports = List.of(
                new OccupancyReport(queries),
                new RevenueReport(queries),
                new TourismTaxReport(queries, tourismTax),
                new RatePlanReport(queries));
        return Api.create(new ReservationService(reservations, messages), messages, reports);
    }

    private static String require(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável de ambiente obrigatória: " + name);
        }
        return value;
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
