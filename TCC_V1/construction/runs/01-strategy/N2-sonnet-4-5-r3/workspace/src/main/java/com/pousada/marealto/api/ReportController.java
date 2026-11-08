package com.pousada.marealto.api;

import com.pousada.marealto.service.ReportService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void getOccupancyReport(Context ctx) {
        try {
            String dateParam = ctx.queryParam("date");
            if (dateParam == null) {
                errorResponse(ctx, 400, "INVALID_REQUEST", "Missing required parameter: date");
                return;
            }

            LocalDate date = LocalDate.parse(dateParam);
            String csv = reportService.generateOccupancyReport(date);

            ctx.contentType("text/csv; charset=utf-8")
               .result(csv);
        } catch (DateTimeParseException e) {
            errorResponse(ctx, 400, "INVALID_REQUEST", "Invalid date format");
        }
    }

    public void getRevenueReport(Context ctx) {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null) {
                errorResponse(ctx, 400, "INVALID_REQUEST", "Missing required parameter: month");
                return;
            }

            YearMonth yearMonth = YearMonth.parse(monthParam);
            String csv = reportService.generateRevenueReport(yearMonth.getYear(), yearMonth.getMonthValue());

            ctx.contentType("text/csv; charset=utf-8")
               .result(csv);
        } catch (DateTimeParseException e) {
            errorResponse(ctx, 400, "INVALID_REQUEST", "Invalid month format");
        }
    }

    private void errorResponse(Context ctx, int status, String code, String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", code);
        error.put("message", message);
        ctx.status(status).json(error);
    }
}
