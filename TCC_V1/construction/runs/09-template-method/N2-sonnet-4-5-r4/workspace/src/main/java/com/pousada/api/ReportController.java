package com.pousada.api;

import com.pousada.report.OccupancyReport;
import com.pousada.report.RatePlansReport;
import com.pousada.report.RevenueReport;
import com.pousada.report.TourismTaxReport;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public class ReportController {
    private final OccupancyReport occupancyReport;
    private final RevenueReport revenueReport;
    private final TourismTaxReport tourismTaxReport;
    private final RatePlansReport ratePlansReport;

    public ReportController(OccupancyReport occupancyReport,
                            RevenueReport revenueReport,
                            TourismTaxReport tourismTaxReport,
                            RatePlansReport ratePlansReport) {
        this.occupancyReport = occupancyReport;
        this.revenueReport = revenueReport;
        this.tourismTaxReport = tourismTaxReport;
        this.ratePlansReport = ratePlansReport;
    }

    public void register(Javalin app) {
        app.get("/reports/occupancy", this::getOccupancyReport);
        app.get("/reports/revenue", this::getRevenueReport);
        app.get("/reports/tourism-tax", this::getTourismTaxReport);
        app.get("/reports/rate-plans", this::getRatePlansReport);
    }

    private void getOccupancyReport(Context ctx) {
        try {
            String dateParam = ctx.queryParam("date");
            if (dateParam == null) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing date parameter"));
                return;
            }

            LocalDate.parse(dateParam);
            String csv = occupancyReport.generate(dateParam);

            ctx.status(HttpStatus.OK);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        }
    }

    private void getRevenueReport(Context ctx) {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            YearMonth.parse(monthParam);
            String csv = revenueReport.generate(monthParam);

            ctx.status(HttpStatus.OK);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }

    private void getTourismTaxReport(Context ctx) {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            YearMonth.parse(monthParam);
            String csv = tourismTaxReport.generate(monthParam);

            ctx.status(HttpStatus.OK);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }

    private void getRatePlansReport(Context ctx) {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            YearMonth.parse(monthParam);
            String csv = ratePlansReport.generate(monthParam);

            ctx.status(HttpStatus.OK);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }
}
