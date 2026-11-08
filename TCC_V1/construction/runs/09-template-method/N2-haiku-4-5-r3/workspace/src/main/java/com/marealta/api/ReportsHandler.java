package com.marealta.api;

import com.marealta.report.Report;
import com.marealta.report.ReportRegistry;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ReportsHandler {
    private static final Logger logger = LoggerFactory.getLogger(ReportsHandler.class);
    private final ReportRegistry reportRegistry;

    public ReportsHandler(ReportRegistry reportRegistry) {
        this.reportRegistry = reportRegistry;
    }

    public void getOccupancyReport(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing date parameter"));
                return;
            }

            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Report report = reportRegistry.get("occupancy");
            String csv = report.generate(date);
            ctx.contentType("text/csv;charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        } catch (Exception e) {
            logger.error("Error generating occupancy report", e);
            ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Error generating report"));
        }
    }

    public void getRevenueReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            YearMonth month = YearMonth.parse(monthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
            Report report = reportRegistry.get("revenue");
            String csv = report.generate(month.atDay(1));
            ctx.contentType("text/csv;charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        } catch (Exception e) {
            logger.error("Error generating revenue report", e);
            ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Error generating report"));
        }
    }

    public void getTourismTaxReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            YearMonth month = YearMonth.parse(monthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
            Report report = reportRegistry.get("tourism-tax");
            String csv = report.generate(month.atDay(1));
            ctx.contentType("text/csv;charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        } catch (Exception e) {
            logger.error("Error generating tourism tax report", e);
            ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Error generating report"));
        }
    }

    public void getRatePlansReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            YearMonth month = YearMonth.parse(monthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
            Report report = reportRegistry.get("rate-plans");
            String csv = report.generate(month.atDay(1));
            ctx.contentType("text/csv;charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        } catch (Exception e) {
            logger.error("Error generating rate plans report", e);
            ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Error generating report"));
        }
    }

    static class ErrorResponse {
        String error;
        String message;

        ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }
}
