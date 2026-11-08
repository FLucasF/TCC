package com.pousada.marealta.handler;

import com.pousada.marealta.service.ReportService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class ReportHandler {
    private final ReportService reportService;

    public ReportHandler(ReportService reportService) {
        this.reportService = reportService;
    }

    public void occupancyReport(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) {
                ErrorHandler.handle400(ctx, "INVALID_REQUEST", "Missing date parameter");
                return;
            }

            try {
                LocalDate date = LocalDate.parse(dateStr);
                String csv = reportService.generateOccupancyReport(date);
                ctx.contentType("text/csv; charset=utf-8").result(csv);
            } catch (DateTimeParseException e) {
                ErrorHandler.handle400(ctx, "INVALID_REQUEST", "Invalid date format");
            }
        } catch (Exception e) {
            ErrorHandler.handleException(ctx, e);
        }
    }

    public void revenueReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                ErrorHandler.handle400(ctx, "INVALID_REQUEST", "Missing month parameter");
                return;
            }

            try {
                String[] parts = monthStr.split("-");
                if (parts.length != 2) {
                    throw new IllegalArgumentException("Invalid month format");
                }
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);

                String csv = reportService.generateRevenueReport(year, month);
                ctx.contentType("text/csv; charset=utf-8").result(csv);
            } catch (NumberFormatException e) {
                ErrorHandler.handle400(ctx, "INVALID_REQUEST", "Invalid month format");
            }
        } catch (Exception e) {
            ErrorHandler.handleException(ctx, e);
        }
    }
}
