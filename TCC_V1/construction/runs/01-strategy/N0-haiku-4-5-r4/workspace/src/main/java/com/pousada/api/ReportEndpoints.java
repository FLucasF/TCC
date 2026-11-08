package com.pousada.api;

import com.pousada.service.ReportService;
import io.javalin.Javalin;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public class ReportEndpoints {
    public static void register(Javalin app) {
        app.get("/reports/occupancy", ctx -> {
            try {
                String dateStr = ctx.queryParam("date");
                if (dateStr == null) {
                    ctx.status(400).json(createError("INVALID_REQUEST", "date parameter is required"));
                    return;
                }

                LocalDate date = LocalDate.parse(dateStr);
                String report = new ReportService().getOccupancyReport(date);
                ctx.contentType("text/csv; charset=utf-8").result(report);
            } catch (DateTimeParseException e) {
                ctx.status(400).json(createError("INVALID_REQUEST", "Invalid date format, use YYYY-MM-DD"));
            } catch (SQLException e) {
                ctx.status(500).json(createError("DATABASE_ERROR", e.getMessage()));
            }
        });

        app.get("/reports/revenue", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null) {
                    ctx.status(400).json(createError("INVALID_REQUEST", "month parameter is required"));
                    return;
                }

                YearMonth month = YearMonth.parse(monthStr);
                String report = new ReportService().getRevenueReport(month);
                ctx.contentType("text/csv; charset=utf-8").result(report);
            } catch (DateTimeParseException e) {
                ctx.status(400).json(createError("INVALID_REQUEST", "Invalid month format, use YYYY-MM"));
            } catch (SQLException e) {
                ctx.status(500).json(createError("DATABASE_ERROR", e.getMessage()));
            }
        });
    }

    private static Object createError(String code, String msg) {
        return new Object() {
            public final String error = code;
            public final String message = msg;
        };
    }
}
