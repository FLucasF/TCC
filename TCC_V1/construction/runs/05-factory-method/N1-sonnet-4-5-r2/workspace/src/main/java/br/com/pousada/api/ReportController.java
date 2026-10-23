package br.com.pousada.api;

import br.com.pousada.exception.InvalidRequestException;
import br.com.pousada.service.ReportService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ReportController {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void getOccupancyReport(Context ctx) {
        String dateParam = ctx.queryParam("date");
        if (dateParam == null) {
            throw new InvalidRequestException("Date parameter is required");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateParam, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid date format. Expected: yyyy-MM-dd");
        }

        String csv = reportService.generateOccupancyReport(date);
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }

    public void getRevenueReport(Context ctx) {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null) {
            throw new InvalidRequestException("Month parameter is required");
        }

        String[] parts = monthParam.split("-");
        if (parts.length != 2) {
            throw new InvalidRequestException("Invalid month format. Expected: yyyy-MM");
        }

        int year;
        int month;
        try {
            year = Integer.parseInt(parts[0]);
            month = Integer.parseInt(parts[1]);
            if (month < 1 || month > 12) {
                throw new InvalidRequestException("Month must be between 1 and 12");
            }
        } catch (NumberFormatException e) {
            throw new InvalidRequestException("Invalid month format. Expected: yyyy-MM");
        }

        String csv = reportService.generateRevenueReport(year, month);
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }
}
