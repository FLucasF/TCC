package br.com.pousada.maralta.controller;

import br.com.pousada.maralta.service.ReportService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void getOccupancyReport(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing date parameter"));
                return;
            }

            LocalDate date;
            try {
                date = LocalDate.parse(dateStr);
            } catch (DateTimeParseException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
                return;
            }

            String csv = reportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);

        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        }
    }

    public void getRevenueReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            YearMonth yearMonth;
            try {
                yearMonth = YearMonth.parse(monthStr);
            } catch (DateTimeParseException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
                return;
            }

            String csv = reportService.generateRevenueReport(yearMonth.getYear(), yearMonth.getMonthValue());
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);

        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        }
    }
}
