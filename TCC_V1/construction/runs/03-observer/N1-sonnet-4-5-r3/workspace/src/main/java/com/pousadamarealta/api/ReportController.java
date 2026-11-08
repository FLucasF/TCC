package com.pousadamarealta.api;

import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void occupancyReport(Context ctx) {
        String dateParam = ctx.queryParam("date");
        if (dateParam == null) {
            throw new ReservationService.InvalidRequestException("Missing date parameter");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateParam);
        } catch (DateTimeParseException e) {
            throw new ReservationService.InvalidRequestException("Invalid date format: " + dateParam);
        }

        String csv = reportService.generateOccupancyReport(date);
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }

    public void revenueReport(Context ctx) {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null) {
            throw new ReservationService.InvalidRequestException("Missing month parameter");
        }

        YearMonth month;
        try {
            month = YearMonth.parse(monthParam);
        } catch (DateTimeParseException e) {
            throw new ReservationService.InvalidRequestException("Invalid month format: " + monthParam);
        }

        String csv = reportService.generateRevenueReport(month);
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }
}
