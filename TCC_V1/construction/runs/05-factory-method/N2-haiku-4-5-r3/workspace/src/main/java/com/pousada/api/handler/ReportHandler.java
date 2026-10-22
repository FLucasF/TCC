package com.pousada.api.handler;

import com.pousada.api.JsonUtil;
import com.pousada.service.ReportService;
import io.javalin.Javalin;
import java.time.LocalDate;
import java.time.YearMonth;

public class ReportHandler {
    public static void register(Javalin app) {
        app.get("/reports/occupancy", ctx -> {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) {
                ctx.status(400);
                ctx.json(JsonUtil.error("INVALID_REQUEST", "Missing date parameter"));
                return;
            }

            try {
                LocalDate date = LocalDate.parse(dateStr);
                String report = ReportService.getInstance().generateOccupancyReport(date);
                ctx.contentType("text/csv;charset=utf-8");
                ctx.result(report);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(JsonUtil.error("INVALID_REQUEST", "Invalid date format"));
            }
        });

        app.get("/reports/revenue", ctx -> {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                ctx.status(400);
                ctx.json(JsonUtil.error("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            try {
                YearMonth ym = YearMonth.parse(monthStr);
                String report = ReportService.getInstance().generateRevenueReport(
                    ym.getYear(), ym.getMonthValue()
                );
                ctx.contentType("text/csv;charset=utf-8");
                ctx.result(report);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(JsonUtil.error("INVALID_REQUEST", "Invalid month format"));
            }
        });
    }
}
