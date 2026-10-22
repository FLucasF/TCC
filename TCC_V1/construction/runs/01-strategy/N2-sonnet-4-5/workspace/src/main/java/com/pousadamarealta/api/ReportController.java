package com.pousadamarealta.api;

import com.pousadamarealta.exception.InvalidRequestException;
import com.pousadamarealta.service.ReportService;
import io.javalin.http.Context;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void getOccupancyReport(Context ctx) {
        String dateParam = ctx.queryParam("date");

        if (dateParam == null || dateParam.isEmpty()) {
            throw new InvalidRequestException("Date parameter is required");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateParam, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid date format. Use YYYY-MM-DD");
        }

        String csv = reportService.generateOccupancyReport(date);

        ctx.contentType("text/csv; charset=utf-8")
           .result(csv);
    }

    public void getRevenueReport(Context ctx) {
        String monthParam = ctx.queryParam("month");

        if (monthParam == null || monthParam.isEmpty()) {
            throw new InvalidRequestException("Month parameter is required");
        }

        YearMonth month;
        try {
            month = YearMonth.parse(monthParam, DateTimeFormatter.ofPattern("yyyy-MM"));
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid month format. Use YYYY-MM");
        }

        String csv = reportService.generateRevenueReport(month);

        ctx.contentType("text/csv; charset=utf-8")
           .result(csv);
    }
}
