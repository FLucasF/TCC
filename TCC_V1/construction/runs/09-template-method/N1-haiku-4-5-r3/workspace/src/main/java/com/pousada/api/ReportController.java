package com.pousada.api;

import com.pousada.exception.ApiException;
import com.pousada.service.ReportService;
import io.javalin.http.Context;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class ReportController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    public static void getOccupancyReport(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null || dateStr.isEmpty()) {
                throw new ApiException(400, "INVALID_REQUEST", "Date parameter is required");
            }

            LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
            String report = ReportService.generateOccupancyReport(date);

            ctx.header("Content-Type", "text/csv; charset=utf-8");
            ctx.result(report);
        } catch (ApiException e) {
            sendErrorResponse(ctx, e);
        } catch (Exception e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid date format");
        }
    }

    public static void getRevenueReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                throw new ApiException(400, "INVALID_REQUEST", "Month parameter is required");
            }

            YearMonth month = YearMonth.parse(monthStr, MONTH_FORMATTER);
            String report = ReportService.generateRevenueReport(month);

            ctx.header("Content-Type", "text/csv; charset=utf-8");
            ctx.result(report);
        } catch (ApiException e) {
            sendErrorResponse(ctx, e);
        } catch (Exception e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid month format");
        }
    }

    public static void getTourismTaxReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                throw new ApiException(400, "INVALID_REQUEST", "Month parameter is required");
            }

            YearMonth month = YearMonth.parse(monthStr, MONTH_FORMATTER);
            String report = ReportService.generateTourismTaxReport(month);

            ctx.header("Content-Type", "text/csv; charset=utf-8");
            ctx.result(report);
        } catch (ApiException e) {
            sendErrorResponse(ctx, e);
        } catch (Exception e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid month format");
        }
    }

    public static void getRatePlansReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                throw new ApiException(400, "INVALID_REQUEST", "Month parameter is required");
            }

            YearMonth month = YearMonth.parse(monthStr, MONTH_FORMATTER);
            String report = ReportService.generateRatePlansReport(month);

            ctx.header("Content-Type", "text/csv; charset=utf-8");
            ctx.result(report);
        } catch (ApiException e) {
            sendErrorResponse(ctx, e);
        } catch (Exception e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid month format");
        }
    }

    private static void sendErrorResponse(Context ctx, ApiException e) {
        ctx.status(e.getStatusCode());
        ctx.json("{\"error\": \"" + e.getErrorCode() + "\", \"message\": \"" + e.getMessage() + "\"}");
    }
}
