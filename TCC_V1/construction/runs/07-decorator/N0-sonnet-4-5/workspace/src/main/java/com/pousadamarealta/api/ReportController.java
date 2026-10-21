package com.pousadamarealta.api;

import com.pousadamarealta.service.ReportService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void getOccupancyReport(Context ctx) {
        String dateParam = ctx.queryParam("date");
        if (dateParam == null || dateParam.isEmpty()) {
            ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "Missing date parameter"
            ));
            return;
        }

        try {
            LocalDate date = LocalDate.parse(dateParam, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String csv = reportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "Invalid date format"
            ));
        }
    }

    public void getRevenueReport(Context ctx) {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null || monthParam.isEmpty()) {
            ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "Missing month parameter"
            ));
            return;
        }

        try {
            YearMonth month = YearMonth.parse(monthParam, DateTimeFormatter.ofPattern("yyyy-MM"));
            String csv = reportService.generateRevenueReport(month);
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "Invalid month format"
            ));
        }
    }
}
