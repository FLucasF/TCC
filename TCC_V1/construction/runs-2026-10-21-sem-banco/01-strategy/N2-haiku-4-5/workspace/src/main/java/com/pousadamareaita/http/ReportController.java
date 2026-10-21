package com.pousadamareaita.http;

import com.pousadamareaita.http.dto.ErrorDto;
import com.pousadamareaita.service.ReportService;
import io.javalin.Javalin;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class ReportController {
    private final ReportService reportService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void registerRoutes(Javalin app) {
        app.get("/reports/occupancy", this::getOccupancyReport);
        app.get("/reports/revenue", this::getRevenueReport);
    }

    private void getOccupancyReport(io.javalin.http.Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null || dateStr.isEmpty()) {
                ctx.status(400);
                ctx.json(new ErrorDto("INVALID_REQUEST", "Missing date parameter"));
                return;
            }

            LocalDate date;
            try {
                date = LocalDate.parse(dateStr, DATE_FORMATTER);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new ErrorDto("INVALID_REQUEST", "Invalid date format"));
                return;
            }

            String csv = reportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ErrorDto("INVALID_REQUEST", e.getMessage()));
        }
    }

    private void getRevenueReport(io.javalin.http.Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                ctx.status(400);
                ctx.json(new ErrorDto("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            YearMonth month;
            try {
                month = YearMonth.parse(monthStr, MONTH_FORMATTER);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new ErrorDto("INVALID_REQUEST", "Invalid month format"));
                return;
            }

            String csv = reportService.generateRevenueReport(month);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ErrorDto("INVALID_REQUEST", e.getMessage()));
        }
    }
}
