package com.pousadamareal.handler;

import com.pousadamareal.service.ReportService;
import com.pousadamareal.util.JsonUtils;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public class ReportHandler {

    public static void occupancy(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null || dateStr.isEmpty()) {
                ctx.status(400).json(JsonUtils.map("error", "INVALID_REQUEST", "message", "date é obrigatório"));
                return;
            }

            LocalDate date = LocalDate.parse(dateStr);
            String report = ReportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8").result(report);
        } catch (DateTimeParseException e) {
            ctx.status(400).json(JsonUtils.map("error", "INVALID_REQUEST", "message", "Formato de data inválido"));
        } catch (SQLException e) {
            ctx.status(500).json(JsonUtils.map("error", "INTERNAL_ERROR", "message", e.getMessage()));
        }
    }

    public static void revenue(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                ctx.status(400).json(JsonUtils.map("error", "INVALID_REQUEST", "message", "month é obrigatório"));
                return;
            }

            YearMonth month = YearMonth.parse(monthStr);
            String report = ReportService.generateRevenueReport(month.getYear(), month.getMonthValue());
            ctx.contentType("text/csv; charset=utf-8").result(report);
        } catch (DateTimeParseException e) {
            ctx.status(400).json(JsonUtils.map("error", "INVALID_REQUEST", "message", "Formato de mês inválido"));
        } catch (SQLException e) {
            ctx.status(500).json(JsonUtils.map("error", "INTERNAL_ERROR", "message", e.getMessage()));
        }
    }
}
