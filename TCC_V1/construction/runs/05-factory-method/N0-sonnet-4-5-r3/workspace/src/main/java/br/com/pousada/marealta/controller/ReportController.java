package br.com.pousada.marealta.controller;

import br.com.pousada.marealta.exception.AppException;
import br.com.pousada.marealta.service.ReportService;
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
            throw AppException.invalidRequest("Parâmetro 'date' é obrigatório");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateParam);
        } catch (DateTimeParseException e) {
            throw AppException.invalidRequest("Formato de data inválido: " + dateParam);
        }

        String csv = reportService.generateOccupancyReport(date);
        ctx.contentType("text/csv; charset=utf-8");
        ctx.result(csv);
    }

    public void getRevenueReport(Context ctx) {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null) {
            throw AppException.invalidRequest("Parâmetro 'month' é obrigatório");
        }

        YearMonth month;
        try {
            month = YearMonth.parse(monthParam);
        } catch (DateTimeParseException e) {
            throw AppException.invalidRequest("Formato de mês inválido: " + monthParam);
        }

        String csv = reportService.generateRevenueReport(month);
        ctx.contentType("text/csv; charset=utf-8");
        ctx.result(csv);
    }
}
