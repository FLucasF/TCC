package com.pousada.api;

import com.pousada.repository.RoomRepository;
import com.pousada.service.ReportService;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class ReportApi {
    private static final Logger log = LoggerFactory.getLogger(ReportApi.class);
    private static final RoomRepository roomRepo = new RoomRepository();
    private static final ReportService reportService = new ReportService(roomRepo);

    public static void register(Javalin app) {
        app.get("/reports/occupancy", ctx -> {
            try {
                String dateStr = ctx.queryParam("date");
                if (dateStr == null || dateStr.isEmpty()) {
                    ctx.status(400).result("Missing date parameter");
                    return;
                }

                LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);
                String report = reportService.generateOccupancyReport(date);

                ctx.header("Content-Type", "text/csv; charset=utf-8");
                ctx.result(report);
            } catch (Exception e) {
                log.error("Error generating occupancy report", e);
                ctx.status(500).result("Error generating report");
            }
        });

        app.get("/reports/revenue", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null || monthStr.isEmpty()) {
                    ctx.status(400).result("Missing month parameter");
                    return;
                }

                YearMonth month = YearMonth.parse(monthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
                String report = reportService.generateRevenueReport(month);

                ctx.header("Content-Type", "text/csv; charset=utf-8");
                ctx.result(report);
            } catch (Exception e) {
                log.error("Error generating revenue report", e);
                ctx.status(500).result("Error generating report");
            }
        });
    }
}
