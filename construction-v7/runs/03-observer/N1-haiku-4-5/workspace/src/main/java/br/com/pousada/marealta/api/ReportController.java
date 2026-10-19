package br.com.pousada.marealta.api;

import br.com.pousada.marealta.service.ReportService;
import io.javalin.Javalin;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public class ReportController {
    public static void register(Javalin app, ReportService reportService) {
        app.get("/reports/occupancy", ctx -> {
            try {
                String dateParam = ctx.queryParam("date");
                if (dateParam == null) {
                    throw new IllegalArgumentException("INVALID_REQUEST");
                }
                LocalDate date = LocalDate.parse(dateParam);
                String csv = reportService.getOccupancyReport(date);
                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(csv);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("INVALID_REQUEST");
            }
        });

        app.get("/reports/revenue", ctx -> {
            try {
                String monthParam = ctx.queryParam("month");
                if (monthParam == null) {
                    throw new IllegalArgumentException("INVALID_REQUEST");
                }
                YearMonth month = YearMonth.parse(monthParam);
                String csv = reportService.getRevenueReport(month);
                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(csv);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("INVALID_REQUEST");
            }
        });
    }
}
