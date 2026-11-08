package br.com.pousadamarealta.api;

import br.com.pousadamarealta.api.dto.ErrorResponse;
import br.com.pousadamarealta.service.ReportService;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public class ReportsController {
    private final ReportService reportService;

    public ReportsController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void register(Javalin app) {
        app.get("/reports/occupancy", ctx -> {
            String dateParam = ctx.queryParam("date");
            if (dateParam == null || dateParam.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing date parameter"));
                return;
            }

            try {
                LocalDate date = LocalDate.parse(dateParam);
                String csv = reportService.generateOccupancyReport(date);
                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(csv);
            } catch (DateTimeParseException e) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
            }
        });

        app.get("/reports/revenue", ctx -> {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null || monthParam.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            try {
                YearMonth month = YearMonth.parse(monthParam);
                String csv = reportService.generateRevenueReport(month);
                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(csv);
            } catch (DateTimeParseException e) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
            }
        });
    }
}
