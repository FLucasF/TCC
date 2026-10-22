package com.pousada.handler;

import com.google.gson.JsonObject;
import com.pousada.service.ReportService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class OccupancyReportHandler {
    private static final Logger logger = LoggerFactory.getLogger(OccupancyReportHandler.class);
    private static final ReportService reportService = new ReportService();
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void getOccupancyReport(Context ctx) {
        try {
            String dateParam = ctx.queryParam("date");
            if (dateParam == null || dateParam.isEmpty()) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "INVALID_REQUEST");
                error.addProperty("message", "date parameter is required");
                ctx.status(400).json(error);
                return;
            }

            LocalDate date = LocalDate.parse(dateParam, ISO_DATE);
            String report = reportService.generateOccupancyReport(date);

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(report);
        } catch (DateTimeParseException e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "INVALID_REQUEST");
            error.addProperty("message", "Invalid date format");
            ctx.status(400).json(error);
        } catch (Exception e) {
            logger.error("Error generating occupancy report", e);
            JsonObject error = new JsonObject();
            error.addProperty("error", "INTERNAL_ERROR");
            error.addProperty("message", "Error generating report");
            ctx.status(500).json(error);
        }
    }
}
