package com.pousada.marealta.api;

import com.pousada.marealta.infrastructure.ReservationRepository;
import com.pousada.marealta.infrastructure.RoomRepository;
import com.pousada.marealta.service.OccupancyReport;
import com.pousada.marealta.service.RevenueReport;
import io.javalin.http.Context;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class ReportController {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    public ReportController(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public void getOccupancyReport(Context ctx) throws Exception {
        String dateParam = ctx.queryParam("date");
        if (dateParam == null || dateParam.isEmpty()) {
            ctx.status(400);
            ctx.result("Missing date parameter");
            return;
        }

        LocalDate date = LocalDate.parse(dateParam, DATE_FORMATTER);
        OccupancyReport report = new OccupancyReport(reservationRepository, roomRepository);
        String csv = report.generate(date);

        ctx.contentType("text/csv; charset=utf-8");
        ctx.result(csv);
    }

    public void getRevenueReport(Context ctx) throws Exception {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null || monthParam.isEmpty()) {
            ctx.status(400);
            ctx.result("Missing month parameter");
            return;
        }

        YearMonth yearMonth = YearMonth.parse(monthParam, MONTH_FORMATTER);
        RevenueReport report = new RevenueReport(reservationRepository);
        String csv = report.generate(yearMonth);

        ctx.contentType("text/csv; charset=utf-8");
        ctx.result(csv);
    }
}
