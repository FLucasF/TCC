package com.pousada.api;

import com.pousada.repo.ReservationRepository;
import com.pousada.repo.RoomRepository;
import com.pousada.service.report.OccupancyReport;
import com.pousada.service.report.RevenueReport;
import com.pousada.service.report.TourismTaxReport;
import com.pousada.service.report.RatePlansReport;
import io.javalin.Javalin;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.YearMonth;

public class ReportRoutes {
    public static void register(Javalin app, ReservationRepository reservationRepo, RoomRepository roomRepo) {
        app.get("/reports/occupancy", ctx -> {
            try {
                String dateStr = ctx.queryParam("date");
                if (dateStr == null || dateStr.isEmpty()) {
                    ctx.status(400);
                    ctx.json(new JSONObject()
                        .put("error", "INVALID_REQUEST")
                        .put("message", "date parameter is required")
                        .toString());
                    return;
                }

                LocalDate date = LocalDate.parse(dateStr);
                OccupancyReport report = new OccupancyReport(reservationRepo, roomRepo, date);
                ctx.contentType("text/csv;charset=utf-8");
                ctx.result(report.generate());
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new JSONObject()
                    .put("error", "INVALID_REQUEST")
                    .put("message", "Invalid date format: " + e.getMessage())
                    .toString());
            }
        });

        app.get("/reports/revenue", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null || monthStr.isEmpty()) {
                    ctx.status(400);
                    ctx.json(new JSONObject()
                        .put("error", "INVALID_REQUEST")
                        .put("message", "month parameter is required")
                        .toString());
                    return;
                }

                YearMonth yearMonth = YearMonth.parse(monthStr);
                RevenueReport report = new RevenueReport(reservationRepo, yearMonth);
                ctx.contentType("text/csv;charset=utf-8");
                ctx.result(report.generate());
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new JSONObject()
                    .put("error", "INVALID_REQUEST")
                    .put("message", "Invalid month format: " + e.getMessage())
                    .toString());
            }
        });

        app.get("/reports/tourism-tax", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null || monthStr.isEmpty()) {
                    ctx.status(400);
                    ctx.json(new JSONObject()
                        .put("error", "INVALID_REQUEST")
                        .put("message", "month parameter is required")
                        .toString());
                    return;
                }

                YearMonth yearMonth = YearMonth.parse(monthStr);
                TourismTaxReport report = new TourismTaxReport(reservationRepo, yearMonth);
                ctx.contentType("text/csv;charset=utf-8");
                ctx.result(report.generate());
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new JSONObject()
                    .put("error", "INVALID_REQUEST")
                    .put("message", "Invalid month format: " + e.getMessage())
                    .toString());
            }
        });

        app.get("/reports/rate-plans", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null || monthStr.isEmpty()) {
                    ctx.status(400);
                    ctx.json(new JSONObject()
                        .put("error", "INVALID_REQUEST")
                        .put("message", "month parameter is required")
                        .toString());
                    return;
                }

                YearMonth yearMonth = YearMonth.parse(monthStr);
                RatePlansReport report = new RatePlansReport(reservationRepo, yearMonth);
                ctx.contentType("text/csv;charset=utf-8");
                ctx.result(report.generate());
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new JSONObject()
                    .put("error", "INVALID_REQUEST")
                    .put("message", "Invalid month format: " + e.getMessage())
                    .toString());
            }
        });
    }
}
