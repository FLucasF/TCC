package com.marealta.api.handlers;

import io.javalin.http.Context;
import com.marealta.service.ReportService;
import com.marealta.api.ErrorResponse;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class ReportHandler {
    public static void occupancy(Context ctx) throws Exception {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing date parameter"));
                return;
            }

            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String report = ReportService.generateOccupancyReport(date);

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(report);
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        }
    }

    public static void revenue(Context ctx) throws Exception {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            YearMonth month = YearMonth.parse(monthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
            String report = ReportService.generateRevenueReport(month);

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(report);
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }
}
