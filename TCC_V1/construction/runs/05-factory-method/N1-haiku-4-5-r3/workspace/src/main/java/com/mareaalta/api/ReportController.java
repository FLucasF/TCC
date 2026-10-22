package com.mareaalta.api;

import com.mareaalta.service.ReportService;
import io.javalin.http.Context;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class ReportController {
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter ISO_MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    public static void getOccupancyReport(Context ctx) throws Exception {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) {
                ctx.status(400).json(errorResponse("INVALID_REQUEST", "Parâmetro date obrigatório"));
                return;
            }

            LocalDate date = LocalDate.parse(dateStr, ISO_DATE);
            String report = ReportService.generateOccupancyReport(date);

            ctx.contentType("text/csv;charset=utf-8").result(report);
        } catch (Exception e) {
            ctx.status(400).json(errorResponse("INVALID_REQUEST", "Data inválida"));
        }
    }

    public static void getRevenueReport(Context ctx) throws Exception {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                ctx.status(400).json(errorResponse("INVALID_REQUEST", "Parâmetro month obrigatório"));
                return;
            }

            YearMonth ym = YearMonth.parse(monthStr, ISO_MONTH);
            String report = ReportService.generateRevenueReport(ym.getYear(), ym.getMonthValue());

            ctx.contentType("text/csv;charset=utf-8").result(report);
        } catch (Exception e) {
            ctx.status(400).json(errorResponse("INVALID_REQUEST", "Mês inválido"));
        }
    }

    private static Object errorResponse(String code, String message) {
        com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
        obj.addProperty("error", code);
        obj.addProperty("message", message);
        return obj;
    }
}
