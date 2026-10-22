package com.pousadamarealta.api;

import com.pousadamarealta.report.*;
import com.pousadamarealta.repository.ReservationRepository;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public class ReportController {
    private final ReservationRepository reservationRepository;
    private final int totalRooms;

    public ReportController(ReservationRepository reservationRepository, int totalRooms) {
        this.reservationRepository = reservationRepository;
        this.totalRooms = totalRooms;
    }

    public void getOccupancyReport(Context ctx) {
        try {
            String dateParam = ctx.queryParam("date");
            if (dateParam == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing date parameter"));
                return;
            }

            LocalDate date = LocalDate.parse(dateParam);
            ReportGenerator report = new OccupancyReportGenerator(date, reservationRepository, totalRooms);
            String csv = report.generate();
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        }
    }

    public void getRevenueReport(Context ctx) {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            YearMonth month = YearMonth.parse(monthParam);
            ReportGenerator report = new RevenueReportGenerator(month, reservationRepository);
            String csv = report.generate();
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }

    public void getTourismTaxReport(Context ctx) {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            YearMonth month = YearMonth.parse(monthParam);
            ReportGenerator report = new TourismTaxReportGenerator(month, reservationRepository);
            String csv = report.generate();
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }

    public void getRatePlansReport(Context ctx) {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null) {
                ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            YearMonth month = YearMonth.parse(monthParam);
            ReportGenerator report = new RatePlansReportGenerator(month, reservationRepository);
            String csv = report.generate();
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }
}
