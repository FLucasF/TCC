package com.pousada.routes;

import com.pousada.database.Database;
import com.pousada.services.ReportService;
import com.pousada.utils.ErrorResponse;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public class ReportRoutes {
    private final ReportService reportService;

    public ReportRoutes(Database db) {
        this.reportService = new ReportService(db);
    }

    public void getOccupancy(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null || dateStr.isEmpty()) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Parâmetro 'date' é obrigatório no formato yyyy-MM-dd"));
                return;
            }

            LocalDate date = LocalDate.parse(dateStr);
            String csv = reportService.getOccupancyReport(date);

            ctx.header("Content-Type", "text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Formato de data inválido, use yyyy-MM-dd"));
        }
    }

    public void getRevenue(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Parâmetro 'month' é obrigatório no formato yyyy-MM"));
                return;
            }

            YearMonth month = YearMonth.parse(monthStr);
            String csv = reportService.getRevenueReport(month);

            ctx.header("Content-Type", "text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Formato de mês inválido, use yyyy-MM"));
        }
    }

    public void getTouristTax(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Parâmetro 'month' é obrigatório no formato yyyy-MM"));
                return;
            }

            YearMonth month = YearMonth.parse(monthStr);
            String csv = reportService.getTouristTaxReport(month);

            ctx.header("Content-Type", "text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Formato de mês inválido, use yyyy-MM"));
        }
    }

    public void getRatePlans(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Parâmetro 'month' é obrigatório no formato yyyy-MM"));
                return;
            }

            YearMonth month = YearMonth.parse(monthStr);
            String csv = reportService.getRatePlansReport(month);

            ctx.header("Content-Type", "text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Formato de mês inválido, use yyyy-MM"));
        }
    }
}
