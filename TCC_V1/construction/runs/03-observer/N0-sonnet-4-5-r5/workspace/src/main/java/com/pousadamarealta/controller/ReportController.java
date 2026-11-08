package com.pousadamarealta.controller;

import com.pousadamarealta.exception.AppException;
import com.pousadamarealta.service.ReportService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ReportController {
    private final ReportService reportService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

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
            date = LocalDate.parse(dateParam, dateFormatter);
        } catch (DateTimeParseException e) {
            throw AppException.invalidRequest("Data inválida: " + dateParam);
        }

        String csv = reportService.generateOccupancyReport(date);
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }

    public void getRevenueReport(Context ctx) {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null) {
            throw AppException.invalidRequest("Parâmetro 'month' é obrigatório");
        }

        YearMonth yearMonth;
        try {
            yearMonth = YearMonth.parse(monthParam, monthFormatter);
        } catch (DateTimeParseException e) {
            throw AppException.invalidRequest("Mês inválido: " + monthParam);
        }

        String csv = reportService.generateRevenueReport(yearMonth.getYear(), yearMonth.getMonthValue());
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }
}
