package com.pousadamarealta.controller;

import com.pousadamarealta.report.OccupancyReport;
import com.pousadamarealta.report.RatePlansReport;
import com.pousadamarealta.report.RevenueReport;
import com.pousadamarealta.report.TourismTaxReport;
import com.pousadamarealta.service.InvalidRequestException;
import com.pousadamarealta.util.DateUtils;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public class ReportController {

    public void occupancyReport(Context ctx) {
        String dateStr = ctx.queryParam("date");
        if (dateStr == null) {
            throw new InvalidRequestException("Missing date parameter");
        }

        try {
            LocalDate date = DateUtils.parseISO(dateStr);
            String csv = new OccupancyReport().generate(date);
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid date format");
        }
    }

    public void revenueReport(Context ctx) {
        String monthStr = ctx.queryParam("month");
        if (monthStr == null) {
            throw new InvalidRequestException("Missing month parameter");
        }

        try {
            YearMonth yearMonth = YearMonth.parse(monthStr);
            String csv = new RevenueReport().generate(yearMonth.getYear(), yearMonth.getMonthValue());
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid month format");
        }
    }

    public void tourismTaxReport(Context ctx) {
        String monthStr = ctx.queryParam("month");
        if (monthStr == null) {
            throw new InvalidRequestException("Missing month parameter");
        }

        try {
            YearMonth yearMonth = YearMonth.parse(monthStr);
            String csv = new TourismTaxReport().generate(yearMonth.getYear(), yearMonth.getMonthValue());
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid month format");
        }
    }

    public void ratePlansReport(Context ctx) {
        String monthStr = ctx.queryParam("month");
        if (monthStr == null) {
            throw new InvalidRequestException("Missing month parameter");
        }

        try {
            YearMonth yearMonth = YearMonth.parse(monthStr);
            String csv = new RatePlansReport().generate(yearMonth.getYear(), yearMonth.getMonthValue());
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid month format");
        }
    }
}
