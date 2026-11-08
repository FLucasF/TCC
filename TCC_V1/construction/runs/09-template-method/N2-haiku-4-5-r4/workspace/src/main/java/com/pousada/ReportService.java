package com.pousada;

public class ReportService {
    public static String generateOccupancy(String dateStr) {
        var report = new OccupancyReport(dateStr);
        return report.generate();
    }

    public static String generateRevenue(String monthStr) {
        var report = new RevenueReport(monthStr);
        return report.generate();
    }

    public static String generateTourismTax(String monthStr) {
        var report = new TourismTaxReport(monthStr);
        return report.generate();
    }

    public static String generateRatePlans(String monthStr) {
        var report = new RatePlansReport(monthStr);
        return report.generate();
    }
}
