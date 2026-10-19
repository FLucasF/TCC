package com.pousadamarealta.handler;

import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ServiceException;
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
            String dateParam = ctx.queryParam("date");
            if (dateParam == null) {
                throw new ServiceException("INVALID_REQUEST", "Missing date parameter", 400);
            }

            LocalDate date = LocalDate.parse(dateParam);
            String csv = reportService.generateOccupancyReport(date);

            ctx.contentType("text/csv; charset=utf-8")
                .result(csv);
        } catch (DateTimeParseException e) {
            throw new ServiceException("INVALID_REQUEST", "Invalid date format", 400);
        }
    }

    public void getRevenueReport(Context ctx) {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null) {
                throw new ServiceException("INVALID_REQUEST", "Missing month parameter", 400);
            }

            YearMonth month = YearMonth.parse(monthParam);
            String csv = reportService.generateRevenueReport(month);

            ctx.contentType("text/csv; charset=utf-8")
                .result(csv);
        } catch (DateTimeParseException e) {
            throw new ServiceException("INVALID_REQUEST", "Invalid month format", 400);
        }
    }
}
