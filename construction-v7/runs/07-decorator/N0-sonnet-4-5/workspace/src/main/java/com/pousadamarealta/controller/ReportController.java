package com.pousadamarealta.controller;

import com.pousadamarealta.model.ApiException;
import com.pousadamarealta.service.ReportService;
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
        if (dateParam == null || dateParam.isEmpty()) {
            throw ApiException.invalidRequest("Parâmetro date é obrigatório");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateParam);
        } catch (DateTimeParseException e) {
            throw ApiException.invalidRequest("Formato de data inválido. Use yyyy-MM-dd");
        }

        String csv = reportService.generateOccupancyReport(date);
        ctx.contentType("text/csv; charset=utf-8");
        ctx.result(csv);
    }

    public void getRevenueReport(Context ctx) {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null || monthParam.isEmpty()) {
            throw ApiException.invalidRequest("Parâmetro month é obrigatório");
        }

        YearMonth yearMonth;
        try {
            yearMonth = YearMonth.parse(monthParam);
        } catch (DateTimeParseException e) {
            throw ApiException.invalidRequest("Formato de mês inválido. Use yyyy-MM");
        }

        String csv = reportService.generateRevenueReport(yearMonth.getYear(), yearMonth.getMonthValue());
        ctx.contentType("text/csv; charset=utf-8");
        ctx.result(csv);
    }
}
