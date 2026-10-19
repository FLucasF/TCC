package com.pousada.api;

import com.pousada.exception.ApiException;
import com.pousada.service.ReportService;
import com.pousada.util.DateHelper;
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
        String dateStr = ctx.queryParam("date");
        if (dateStr == null || dateStr.isEmpty()) {
            ctx.status(400);
            ctx.json(new ReservationController.ErrorResponse("INVALID_REQUEST", "Date parameter is required"));
            return;
        }

        try {
            LocalDate date = DateHelper.parseDate(dateStr);
            String report = reportService.generateOccupancyReport(date);
            ctx.contentType("text/csv;charset=utf-8");
            ctx.result(report);
        } catch (DateTimeParseException e) {
            ctx.status(400);
            ctx.json(new ReservationController.ErrorResponse("INVALID_REQUEST", "Invalid date format. Use yyyy-MM-dd"));
        }
    }

    public void getRevenueReport(Context ctx) {
        String monthStr = ctx.queryParam("month");
        if (monthStr == null || monthStr.isEmpty()) {
            ctx.status(400);
            ctx.json(new ReservationController.ErrorResponse("INVALID_REQUEST", "Month parameter is required"));
            return;
        }

        try {
            YearMonth month = DateHelper.parseYearMonth(monthStr);
            String report = reportService.generateRevenueReport(month);
            ctx.contentType("text/csv;charset=utf-8");
            ctx.result(report);
        } catch (DateTimeParseException e) {
            ctx.status(400);
            ctx.json(new ReservationController.ErrorResponse("INVALID_REQUEST", "Invalid month format. Use yyyy-MM"));
        }
    }
}
