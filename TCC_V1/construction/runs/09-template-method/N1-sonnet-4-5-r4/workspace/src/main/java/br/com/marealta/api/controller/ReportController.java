package br.com.marealta.api.controller;

import br.com.marealta.report.OccupancyReport;
import br.com.marealta.report.RatePlansReport;
import br.com.marealta.report.RevenueReport;
import br.com.marealta.report.TourismTaxReport;
import io.javalin.http.Context;

public class ReportController {
    private final OccupancyReport occupancyReport;
    private final RevenueReport revenueReport;
    private final TourismTaxReport tourismTaxReport;
    private final RatePlansReport ratePlansReport;

    public ReportController(
            OccupancyReport occupancyReport,
            RevenueReport revenueReport,
            TourismTaxReport tourismTaxReport,
            RatePlansReport ratePlansReport) {
        this.occupancyReport = occupancyReport;
        this.revenueReport = revenueReport;
        this.tourismTaxReport = tourismTaxReport;
        this.ratePlansReport = ratePlansReport;
    }

    public void getOccupancyReport(Context ctx) {
        String date = ctx.queryParam("date");
        String csv = occupancyReport.generate(date);
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }

    public void getRevenueReport(Context ctx) {
        String month = ctx.queryParam("month");
        String csv = revenueReport.generate(month);
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }

    public void getTourismTaxReport(Context ctx) {
        String month = ctx.queryParam("month");
        String csv = tourismTaxReport.generate(month);
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }

    public void getRatePlansReport(Context ctx) {
        String month = ctx.queryParam("month");
        String csv = ratePlansReport.generate(month);
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }
}
