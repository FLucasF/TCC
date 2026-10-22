package br.com.pousadamarealta.api;

import br.com.pousadamarealta.api.dto.ErrorResponse;
import br.com.pousadamarealta.service.ReportService;
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
        try {
            String dateParam = ctx.queryParam("date");
            if (dateParam == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Date parameter is required"));
                return;
            }

            LocalDate date = LocalDate.parse(dateParam, DateTimeFormatter.ISO_LOCAL_DATE);
            String csv = reportService.generateOccupancyReport(date);

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        }
    }

    public void getRevenueReport(Context ctx) {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Month parameter is required"));
                return;
            }

            YearMonth yearMonth = YearMonth.parse(monthParam, DateTimeFormatter.ofPattern("yyyy-MM"));
            String csv = reportService.generateRevenueReport(yearMonth.getYear(), yearMonth.getMonthValue());

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }
}
