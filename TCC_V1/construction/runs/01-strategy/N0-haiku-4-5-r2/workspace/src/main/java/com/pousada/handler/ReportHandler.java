package com.pousada.handler;

import com.pousada.service.ReportService;
import com.pousada.util.ErrorResponse;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class ReportHandler {
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_DATE;
    private static final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

    public static void occupancyReport(Context ctx) {
        try {
            String dateParam = ctx.queryParam("date");
            if (dateParam == null || dateParam.isEmpty()) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing date parameter"));
                return;
            }

            LocalDate date = LocalDate.parse(dateParam, dateFormatter);
            String report = ReportService.getOccupancyReport(date);

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(report);

        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        }
    }

    public static void revenueReport(Context ctx) {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null || monthParam.isEmpty()) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            YearMonth month = YearMonth.parse(monthParam, monthFormatter);
            String report = ReportService.getRevenueReport(month);

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(report);

        } catch (Exception e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }
}
