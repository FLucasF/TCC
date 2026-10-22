package br.com.marealte.controller;

import br.com.marealte.dto.ErrorResponse;
import br.com.marealte.service.ReportService;
import io.javalin.http.Context;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void getOccupancyReport(Context ctx) {
        String dateParam = ctx.queryParam("date");

        if (dateParam == null) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Date parameter is required"));
            return;
        }

        try {
            LocalDate date = LocalDate.parse(dateParam);
            String csv = reportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        }
    }

    public void getRevenueReport(Context ctx) {
        String monthParam = ctx.queryParam("month");

        if (monthParam == null) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Month parameter is required"));
            return;
        }

        try {
            YearMonth month = YearMonth.parse(monthParam);
            String csv = reportService.generateRevenueReport(month);
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }
}
