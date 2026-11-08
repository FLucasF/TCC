package com.pousada.marealba.controller;

import com.pousada.marealba.exception.ApiException;
import com.pousada.marealba.service.ReportService;
import com.pousada.marealba.util.DateUtil;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void getOccupancyReport(Context ctx) {
        String dateParam = ctx.queryParam("date");

        if (dateParam == null || dateParam.trim().isEmpty()) {
            throw ApiException.invalidRequest("Missing required parameter: date");
        }

        try {
            LocalDate date = DateUtil.parseIso(dateParam);
            String csv = reportService.generateOccupancyReport(date);

            ctx.contentType("text/csv; charset=utf-8")
               .result(csv);

        } catch (DateTimeParseException e) {
            throw ApiException.invalidRequest("Invalid date format");
        }
    }

    public void getRevenueReport(Context ctx) {
        String monthParam = ctx.queryParam("month");

        if (monthParam == null || monthParam.trim().isEmpty()) {
            throw ApiException.invalidRequest("Missing required parameter: month");
        }

        try {
            String[] parts = monthParam.split("-");
            if (parts.length != 2) {
                throw new IllegalArgumentException();
            }

            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);

            String csv = reportService.generateRevenueReport(year, month);

            ctx.contentType("text/csv; charset=utf-8")
               .result(csv);

        } catch (Exception e) {
            throw ApiException.invalidRequest("Invalid month format");
        }
    }

    public void getTourismTaxReport(Context ctx) {
        String monthParam = ctx.queryParam("month");

        if (monthParam == null || monthParam.trim().isEmpty()) {
            throw ApiException.invalidRequest("Missing required parameter: month");
        }

        try {
            String[] parts = monthParam.split("-");
            if (parts.length != 2) {
                throw new IllegalArgumentException();
            }

            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);

            String csv = reportService.generateTourismTaxReport(year, month);

            ctx.contentType("text/csv; charset=utf-8")
               .result(csv);

        } catch (Exception e) {
            throw ApiException.invalidRequest("Invalid month format");
        }
    }

    public void getRatePlansReport(Context ctx) {
        String monthParam = ctx.queryParam("month");

        if (monthParam == null || monthParam.trim().isEmpty()) {
            throw ApiException.invalidRequest("Missing required parameter: month");
        }

        try {
            String[] parts = monthParam.split("-");
            if (parts.length != 2) {
                throw new IllegalArgumentException();
            }

            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);

            String csv = reportService.generateRatePlansReport(year, month);

            ctx.contentType("text/csv; charset=utf-8")
               .result(csv);

        } catch (Exception e) {
            throw ApiException.invalidRequest("Invalid month format");
        }
    }
}
