package com.pousada.handler;

import com.google.gson.JsonObject;
import com.pousada.service.ReportService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class RevenueReportHandler {
    private static final Logger logger = LoggerFactory.getLogger(RevenueReportHandler.class);
    private static final ReportService reportService = new ReportService();
    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    public static void getRevenueReport(Context ctx) {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null || monthParam.isEmpty()) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "INVALID_REQUEST");
                error.addProperty("message", "month parameter is required");
                ctx.status(400).json(error);
                return;
            }

            YearMonth yearMonth = YearMonth.parse(monthParam, YEAR_MONTH);
            String report = reportService.generateRevenueReport(yearMonth.getYear(), yearMonth.getMonthValue());

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(report);
        } catch (DateTimeParseException e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "INVALID_REQUEST");
            error.addProperty("message", "Invalid month format");
            ctx.status(400).json(error);
        } catch (Exception e) {
            logger.error("Error generating revenue report", e);
            JsonObject error = new JsonObject();
            error.addProperty("error", "INTERNAL_ERROR");
            error.addProperty("message", "Error generating report");
            ctx.status(500).json(error);
        }
    }
}
