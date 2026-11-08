package com.pousadamarealta.infrastructure.web;

import com.pousadamarealta.domain.service.ReportService;
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
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null || dateStr.isEmpty()) {
                ctx.status(400).json(new ErrorResponse(
                    ErrorCode.INVALID_REQUEST.name(),
                    "Parâmetro 'date' é obrigatório"
                ));
                return;
            }

            LocalDate date = LocalDate.parse(dateStr);
            String csv = reportService.generateOccupancyReport(date);

            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse(
                ErrorCode.INVALID_REQUEST.name(),
                "Formato de data inválido. Use yyyy-MM-dd"
            ));
        }
    }

    public void getRevenueReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                ctx.status(400).json(new ErrorResponse(
                    ErrorCode.INVALID_REQUEST.name(),
                    "Parâmetro 'month' é obrigatório"
                ));
                return;
            }

            YearMonth yearMonth = YearMonth.parse(monthStr);
            String csv = reportService.generateRevenueReport(yearMonth.getYear(), yearMonth.getMonthValue());

            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse(
                ErrorCode.INVALID_REQUEST.name(),
                "Formato de mês inválido. Use yyyy-MM"
            ));
        }
    }
}
