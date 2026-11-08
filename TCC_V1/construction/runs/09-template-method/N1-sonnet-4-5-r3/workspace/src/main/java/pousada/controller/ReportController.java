package pousada.controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import pousada.dto.ErrorResponse;
import pousada.report.*;
import pousada.repository.ReservationRepository;
import pousada.repository.RoomRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public class ReportController {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final CsvReportWriter reportWriter;

    public ReportController(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.reportWriter = new CsvReportWriter();
    }

    public void getOccupancyReport(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                    .json(new ErrorResponse("INVALID_REQUEST", "Missing date parameter"));
                return;
            }

            LocalDate date = LocalDate.parse(dateStr);
            Report report = new OccupancyReport(date, reservationRepository, roomRepository);
            String csv = reportWriter.write(report);

            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                .json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        }
    }

    public void getRevenueReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                    .json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            YearMonth month = YearMonth.parse(monthStr);
            Report report = new RevenueReport(month, reservationRepository);
            String csv = reportWriter.write(report);

            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                .json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }

    public void getTourismTaxReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                    .json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            YearMonth month = YearMonth.parse(monthStr);
            Report report = new TourismTaxReport(month, reservationRepository);
            String csv = reportWriter.write(report);

            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                .json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }

    public void getRatePlanReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                    .json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
                return;
            }

            YearMonth month = YearMonth.parse(monthStr);
            Report report = new RatePlanReport(month, reservationRepository);
            String csv = reportWriter.write(report);

            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                .json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }
}
