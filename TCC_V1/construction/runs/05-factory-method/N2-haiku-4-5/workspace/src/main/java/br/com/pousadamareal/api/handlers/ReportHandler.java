package br.com.pousadamareal.api.handlers;

import br.com.pousadamareal.api.dto.ReservationDTO;
import br.com.pousadamareal.service.ReportService;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class ReportHandler {
    private final ReportService reportService;

    public ReportHandler(ReportService reportService) {
        this.reportService = reportService;
    }

    public void getOccupancyReport(Context ctx) throws SQLException {
        String dateStr = ctx.queryParam("date");
        if (dateStr == null) {
            ctx.status(400);
            ctx.json(ReservationDTO.errorResponse("INVALID_REQUEST", "Missing 'date' parameter"));
            return;
        }

        try {
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            String report = reportService.getOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(report);
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(ReservationDTO.errorResponse("INVALID_REQUEST", "Invalid date format (use YYYY-MM-DD)"));
        }
    }

    public void getRevenueReport(Context ctx) throws SQLException {
        String monthStr = ctx.queryParam("month");
        if (monthStr == null) {
            ctx.status(400);
            ctx.json(ReservationDTO.errorResponse("INVALID_REQUEST", "Missing 'month' parameter"));
            return;
        }

        try {
            YearMonth yearMonth = YearMonth.parse(monthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
            String report = reportService.getRevenueReport(yearMonth.getYear(), yearMonth.getMonthValue());
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(report);
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(ReservationDTO.errorResponse("INVALID_REQUEST", "Invalid month format (use YYYY-MM)"));
        }
    }
}
