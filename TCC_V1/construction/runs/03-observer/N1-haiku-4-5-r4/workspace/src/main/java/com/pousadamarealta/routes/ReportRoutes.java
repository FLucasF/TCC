package com.pousadamarealta.routes;

import io.javalin.Javalin;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.ErrorResponse;

import java.time.LocalDate;
import java.time.YearMonth;

public class ReportRoutes {
    public static void register(Javalin app) throws Exception {
        ReservationRepository reservationRepository = new ReservationRepository();
        RoomRepository roomRepository = new RoomRepository();
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        app.get("/reports/occupancy", ctx -> {
            try {
                String dateStr = ctx.queryParam("date");
                if (dateStr == null || dateStr.isEmpty()) {
                    ctx.status(400);
                    ctx.json(new ErrorResponse("INVALID_REQUEST", "date parameter is required"));
                    return;
                }

                LocalDate date = LocalDate.parse(dateStr);
                String csv = reportService.generateOccupancyReport(date);

                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(csv);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
            }
        });

        app.get("/reports/revenue", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null || monthStr.isEmpty()) {
                    ctx.status(400);
                    ctx.json(new ErrorResponse("INVALID_REQUEST", "month parameter is required"));
                    return;
                }

                YearMonth yearMonth = YearMonth.parse(monthStr);
                String csv = reportService.generateRevenueReport(yearMonth.getYear(), yearMonth.getMonthValue());

                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(csv);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid month format (expected yyyy-MM)"));
            }
        });
    }
}
