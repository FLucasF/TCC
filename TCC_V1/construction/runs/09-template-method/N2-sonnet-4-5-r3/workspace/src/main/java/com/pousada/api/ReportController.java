package com.pousada.api;

import com.pousada.report.*;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ReportController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportController(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public void getOccupancyReport(Context ctx) {
        try {
            String dateParam = ctx.queryParam("date");
            if (dateParam == null) {
                throw new IllegalArgumentException("Missing date parameter");
            }

            LocalDate date = LocalDate.parse(dateParam, DATE_FORMATTER);

            Report report = new OccupancyReport(date, reservationRepository, roomRepository);
            String csv = report.generate();

            ctx.contentType("text/csv; charset=utf-8").result(csv);

        } catch (DateTimeParseException | IllegalArgumentException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        }
    }

    public void getRevenueReport(Context ctx) {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null) {
                throw new IllegalArgumentException("Missing month parameter");
            }

            YearMonth yearMonth = YearMonth.parse(monthParam, MONTH_FORMATTER);

            Report report = new RevenueReport(yearMonth.getYear(), yearMonth.getMonthValue(), reservationRepository);
            String csv = report.generate();

            ctx.contentType("text/csv; charset=utf-8").result(csv);

        } catch (DateTimeParseException | IllegalArgumentException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }

    public void getTourismTaxReport(Context ctx) {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null) {
                throw new IllegalArgumentException("Missing month parameter");
            }

            YearMonth yearMonth = YearMonth.parse(monthParam, MONTH_FORMATTER);

            Report report = new TourismTaxReport(yearMonth.getYear(), yearMonth.getMonthValue(), reservationRepository);
            String csv = report.generate();

            ctx.contentType("text/csv; charset=utf-8").result(csv);

        } catch (DateTimeParseException | IllegalArgumentException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }

    public void getRatePlansReport(Context ctx) {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null) {
                throw new IllegalArgumentException("Missing month parameter");
            }

            YearMonth yearMonth = YearMonth.parse(monthParam, MONTH_FORMATTER);

            Report report = new RatePlansReport(yearMonth.getYear(), yearMonth.getMonthValue(), reservationRepository);
            String csv = report.generate();

            ctx.contentType("text/csv; charset=utf-8").result(csv);

        } catch (DateTimeParseException | IllegalArgumentException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }
}
