package com.pousada.controller;

import com.pousada.dto.ErrorResponse;
import com.pousada.service.ReportService;
import io.javalin.Javalin;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ReportController {
    private final ReportService reportService;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void registerRoutes(Javalin app) {
        app.get("/reports/occupancy", this::getOccupancyReport);
        app.get("/reports/revenue", this::getRevenueReport);
    }

    private void getOccupancyReport(io.javalin.http.Context ctx) {
        String dateParam = ctx.queryParam("date");

        if (dateParam == null) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "date parameter is required"));
            return;
        }

        try {
            LocalDate date = LocalDate.parse(dateParam, DATE_FORMAT);
            String report = reportService.generateOccupancyReport(date);
            ctx.header("Content-Type", "text/csv;charset=utf-8");
            ctx.result(report);
        } catch (DateTimeParseException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid date format, use yyyy-MM-dd"));
        }
    }

    private void getRevenueReport(io.javalin.http.Context ctx) {
        String monthParam = ctx.queryParam("month");

        if (monthParam == null) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "month parameter is required"));
            return;
        }

        try {
            String[] parts = monthParam.split("-");
            if (parts.length != 2) {
                throw new NumberFormatException();
            }
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);

            if (month < 1 || month > 12) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid month"));
                return;
            }

            String report = reportService.generateRevenueReport(year, month);
            ctx.header("Content-Type", "text/csv;charset=utf-8");
            ctx.result(report);
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid month format, use yyyy-MM"));
        }
    }
}
