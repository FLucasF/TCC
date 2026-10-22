package com.pousada.marealta.api;

import com.pousada.marealta.repository.ReservationRepository;
import com.pousada.marealta.repository.RoomRepository;
import com.pousada.marealta.service.ReportService;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public class ReportsHandler extends Handler {
    private final ReportService reportService;

    public ReportsHandler(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reportService = new ReportService(reservationRepository, roomRepository);
    }

    public void getOccupancyReport(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null || dateStr.isEmpty()) {
                throw new BadRequestResponse("Missing required parameter: date");
            }

            LocalDate date = parseDate(dateStr);
            String csv = reportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (Exception e) {
            handleException(ctx, e);
        }
    }

    public void getRevenueReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                throw new BadRequestResponse("Missing required parameter: month");
            }

            try {
                YearMonth ym = YearMonth.parse(monthStr);
                String csv = reportService.generateRevenueReport(ym.getYear(), ym.getMonthValue());
                ctx.contentType("text/csv; charset=utf-8").result(csv);
            } catch (DateTimeParseException e) {
                throw new BadRequestResponse("Invalid month format: " + monthStr);
            }
        } catch (Exception e) {
            handleException(ctx, e);
        }
    }
}
