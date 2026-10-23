package com.pousadamarealta.controller;

import com.pousadamarealta.service.ReportService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void getOccupancy(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) {
                ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "Missing date parameter"
                ));
                return;
            }

            LocalDate date = LocalDate.parse(dateStr);
            String csv = reportService.generateOccupancyReport(date);

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(400).json(Map.of(
                "error", "INVALID_REQUEST",
                "message", "Invalid date format"
            ));
        }
    }

    public void getRevenue(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "Missing month parameter"
                ));
                return;
            }

            String[] parts = monthStr.split("-");
            if (parts.length != 2) {
                ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "Invalid month format"
                ));
                return;
            }

            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);

            String csv = reportService.generateRevenueReport(year, month);

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (NumberFormatException e) {
            ctx.status(400).json(Map.of(
                "error", "INVALID_REQUEST",
                "message", "Invalid month format"
            ));
        }
    }
}
