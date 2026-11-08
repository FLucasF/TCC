package br.com.pousadamarealta.api;

import br.com.pousadamarealta.domain.reservation.ReservationRepository;
import br.com.pousadamarealta.report.*;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;

public class ReportController {
    private final ReservationRepository reservationRepository;

    public ReportController(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    public void occupancy(Context ctx) {
        try {
            String dateParam = ctx.queryParam("date");
            if (dateParam == null) {
                ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "Missing date parameter"
                ));
                return;
            }

            LocalDate date = LocalDate.parse(dateParam);
            OccupancyReport report = new OccupancyReport(reservationRepository, date);
            String csv = report.generate(dateParam);

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);

        } catch (DateTimeParseException e) {
            ctx.status(400).json(Map.of(
                "error", "INVALID_REQUEST",
                "message", "Invalid date format"
            ));
        }
    }

    public void revenue(Context ctx) {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null) {
                ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "Missing month parameter"
                ));
                return;
            }

            YearMonth yearMonth = YearMonth.parse(monthParam);
            RevenueReport report = new RevenueReport(reservationRepository,
                                                     yearMonth.getYear(),
                                                     yearMonth.getMonthValue());
            String csv = report.generate(monthParam);

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);

        } catch (DateTimeParseException e) {
            ctx.status(400).json(Map.of(
                "error", "INVALID_REQUEST",
                "message", "Invalid month format"
            ));
        }
    }

    public void tourismTax(Context ctx) {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null) {
                ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "Missing month parameter"
                ));
                return;
            }

            YearMonth yearMonth = YearMonth.parse(monthParam);
            TourismTaxReport report = new TourismTaxReport(reservationRepository,
                                                           yearMonth.getYear(),
                                                           yearMonth.getMonthValue());
            String csv = report.generate(monthParam);

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);

        } catch (DateTimeParseException e) {
            ctx.status(400).json(Map.of(
                "error", "INVALID_REQUEST",
                "message", "Invalid month format"
            ));
        }
    }

    public void ratePlans(Context ctx) {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null) {
                ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "Missing month parameter"
                ));
                return;
            }

            YearMonth yearMonth = YearMonth.parse(monthParam);
            RatePlanReport report = new RatePlanReport(reservationRepository,
                                                       yearMonth.getYear(),
                                                       yearMonth.getMonthValue());
            String csv = report.generate(monthParam);

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);

        } catch (DateTimeParseException e) {
            ctx.status(400).json(Map.of(
                "error", "INVALID_REQUEST",
                "message", "Invalid month format"
            ));
        }
    }
}
