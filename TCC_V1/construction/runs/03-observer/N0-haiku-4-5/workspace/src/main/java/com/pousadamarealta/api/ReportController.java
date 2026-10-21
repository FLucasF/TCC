package com.pousadamarealta.api;

import com.pousadamarealta.api.dto.ErrorResponse;
import com.pousadamarealta.service.ReportService;
import io.javalin.http.Context;
import java.time.LocalDate;
import java.time.YearMonth;

public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void getOccupancyReport(Context ctx) {
        String dateStr = ctx.queryParam("date");

        if (dateStr == null || dateStr.isEmpty()) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Parâmetro 'date' é obrigatório"));
            return;
        }

        try {
            LocalDate date = LocalDate.parse(dateStr);
            String report = reportService.getOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8").result(report);
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Formato de data inválido"));
        }
    }

    public void getRevenueReport(Context ctx) {
        String monthStr = ctx.queryParam("month");

        if (monthStr == null || monthStr.isEmpty()) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Parâmetro 'month' é obrigatório"));
            return;
        }

        try {
            YearMonth month = YearMonth.parse(monthStr);
            String report = reportService.getRevenueReport(month);
            ctx.contentType("text/csv; charset=utf-8").result(report);
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Formato de mês inválido"));
        }
    }
}
