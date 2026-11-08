package com.pousadamarealta.route;

import com.pousadamarealta.dto.ErrorResponse;
import com.pousadamarealta.service.ReportService;
import io.javalin.Javalin;

import java.time.LocalDate;
import java.time.YearMonth;

public class ReportRoutes {
    public static void register(Javalin app, ReportService reportService) {
        app.get("/reports/occupancy", ctx -> {
            try {
                String dateStr = ctx.queryParam("date");
                if (dateStr == null || dateStr.isEmpty()) {
                    ctx.status(400);
                    ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing 'date' parameter"));
                    return;
                }

                LocalDate date = LocalDate.parse(dateStr);
                String csv = reportService.generateOccupancyReport(date);
                ctx.header("Content-Type", "text/csv; charset=utf-8");
                ctx.result(csv);
            } catch (IllegalArgumentException e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
            } catch (Exception e) {
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });

        app.get("/reports/revenue", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null || monthStr.isEmpty()) {
                    ctx.status(400);
                    ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing 'month' parameter"));
                    return;
                }

                YearMonth yearMonth = YearMonth.parse(monthStr);
                String csv = reportService.generateRevenueReport(yearMonth.getYear(), yearMonth.getMonthValue());
                ctx.header("Content-Type", "text/csv; charset=utf-8");
                ctx.result(csv);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
            }
        });
    }
}
