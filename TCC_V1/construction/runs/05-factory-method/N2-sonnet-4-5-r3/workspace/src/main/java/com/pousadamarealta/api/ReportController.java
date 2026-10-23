package com.pousadamarealta.api;

import com.pousadamarealta.service.ReportService;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void getOccupancyReport(Context ctx) throws SQLException {
        try {
            String dateParam = ctx.queryParam("date");
            if (dateParam == null) {
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

    public void getRevenueReport(Context ctx) throws SQLException {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            YearMonth yearMonth = YearMonth.parse(monthParam);
            String csv = reportService.generateRevenueReport(yearMonth.getYear(), yearMonth.getMonthValue());

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);

        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }
}
