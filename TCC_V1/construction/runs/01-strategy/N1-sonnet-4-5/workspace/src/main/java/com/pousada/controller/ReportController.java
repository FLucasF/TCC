package com.pousada.controller;

import com.pousada.exception.InvalidRequestException;
import com.pousada.service.ReportService;
import io.javalin.Javalin;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void registerRoutes(Javalin app) {
        app.get("/reports/occupancy", ctx -> {
            String dateParam = ctx.queryParam("date");
            if (dateParam == null) {
                throw new InvalidRequestException("Parâmetro 'date' obrigatório");
            }

            LocalDate date;
            try {
                date = LocalDate.parse(dateParam);
            } catch (DateTimeParseException e) {
                throw new InvalidRequestException("Formato de data inválido");
            }

            String csv = reportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        });

        app.get("/reports/revenue", ctx -> {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null) {
                throw new InvalidRequestException("Parâmetro 'month' obrigatório");
            }

            YearMonth month;
            try {
                month = YearMonth.parse(monthParam);
            } catch (DateTimeParseException e) {
                throw new InvalidRequestException("Formato de mês inválido");
            }

            String csv = reportService.generateRevenueReport(month);
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        });
    }
}
