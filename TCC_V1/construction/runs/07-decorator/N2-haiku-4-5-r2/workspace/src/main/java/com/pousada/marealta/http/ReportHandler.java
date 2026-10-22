package com.pousada.marealta.http;

import com.pousada.marealta.service.ReportService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class ReportHandler {
    private final ReportService reportService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

    public ReportHandler(ReportService reportService) {
        this.reportService = reportService;
    }

    public void occupancyReport(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null || dateStr.isBlank()) {
                ctx.status(HttpStatus.BAD_REQUEST);
                return;
            }

            LocalDate date = LocalDate.parse(dateStr, dateFormatter);
            String csv = reportService.generateOccupancyReport(date);

            ctx.header("Content-Type", "text/csv;charset=utf-8");
            ctx.result(csv);
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST);
        }
    }

    public void revenueReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isBlank()) {
                ctx.status(HttpStatus.BAD_REQUEST);
                return;
            }

            YearMonth yearMonth = YearMonth.parse(monthStr);
            String csv = reportService.generateRevenueReport(yearMonth);

            ctx.header("Content-Type", "text/csv;charset=utf-8");
            ctx.result(csv);
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST);
        }
    }
}
