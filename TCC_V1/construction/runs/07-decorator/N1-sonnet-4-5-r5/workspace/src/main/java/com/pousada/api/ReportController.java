package com.pousada.api;

import com.pousada.service.ReportService;
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
            String dateParam = ctx.queryParam("date");
            if (dateParam == null || dateParam.isEmpty()) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing date parameter"));
                return;
            }

            LocalDate date = LocalDate.parse(dateParam);
            String csv = reportService.generateOccupancyReport(date);

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);

        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        }
    }

    public void getRevenueReport(Context ctx) {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null || monthParam.isEmpty()) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            YearMonth month = YearMonth.parse(monthParam);
            String csv = reportService.generateRevenueReport(month);

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);

        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }
}
