package com.pousadamarealta.handler;

import com.pousadamarealta.dto.ErrorResponse;
import com.pousadamarealta.service.ReportService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public class ReportHandler {
    private final ReportService reportService;

    public ReportHandler(ReportService reportService) {
        this.reportService = reportService;
    }

    public void getOccupancyReport(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing date parameter"));
                return;
            }

            LocalDate date;
            try {
                date = LocalDate.parse(dateStr);
            } catch (DateTimeParseException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
                return;
            }

            String csv = reportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);

        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        }
    }

    public void getRevenueReport(Context ctx) {
        try {
            String month = ctx.queryParam("month");
            if (month == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            try {
                YearMonth.parse(month);
            } catch (DateTimeParseException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
                return;
            }

            String csv = reportService.generateRevenueReport(month);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);

        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        }
    }

    public void getTourismTaxReport(Context ctx) {
        try {
            String month = ctx.queryParam("month");
            if (month == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            try {
                YearMonth.parse(month);
            } catch (DateTimeParseException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
                return;
            }

            String csv = reportService.generateTourismTaxReport(month);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);

        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        }
    }

    public void getRatePlansReport(Context ctx) {
        try {
            String month = ctx.queryParam("month");
            if (month == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            try {
                YearMonth.parse(month);
            } catch (DateTimeParseException e) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
                return;
            }

            String csv = reportService.generateRatePlansReport(month);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);

        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
        }
    }
}
