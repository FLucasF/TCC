package com.pousadamarealta.api.controller;

import com.pousadamarealta.api.ApiException;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class ReportController {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;

    public ReportController(RoomRepository roomRepository, ReservationRepository reservationRepository) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
    }

    public void getOccupancyReport(Context ctx) {
        String dateParam = ctx.queryParam("date");
        if (dateParam == null || dateParam.isBlank()) {
            throw new ApiException(400, "INVALID_REQUEST", "Date parameter is required");
        }

        LocalDate date = parseDate(dateParam);
        List<Reservation> reservations = reservationRepository.findPaidReservationsForNight(date);
        int totalRooms = roomRepository.findAll().size();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : reservations) {
            csv.append(String.format("%s,%s,%d,%s,%s\n",
                reservation.getRoomNumber(),
                reservation.getRoomType().name(),
                reservation.getId(),
                reservation.getCheckIn().format(DATE_FORMAT),
                reservation.getCheckOut().format(DATE_FORMAT)
            ));
        }

        csv.append(String.format("total,%d/%d\n", reservations.size(), totalRooms));

        ctx.contentType("text/csv; charset=utf-8")
           .result(csv.toString());
    }

    public void getRevenueReport(Context ctx) {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null || monthParam.isBlank()) {
            throw new ApiException(400, "INVALID_REQUEST", "Month parameter is required");
        }

        YearMonth yearMonth = parseMonth(monthParam);
        List<Reservation> reservations = reservationRepository.findReservationsForRevenueReport(
            yearMonth.getYear(), yearMonth.getMonthValue()
        );

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal amount = reservation.getAmountPaid();
            if (reservation.getRefund() != null) {
                amount = amount.subtract(reservation.getRefund());
            }

            csv.append(String.format("%d,%s,%s,%s,%s,%.2f\n",
                reservation.getId(),
                reservation.getRoomNumber(),
                reservation.getCheckIn().format(DATE_FORMAT),
                reservation.getCheckOut().format(DATE_FORMAT),
                reservation.getStatus().name(),
                amount.doubleValue()
            ));

            total = total.add(amount);
        }

        csv.append(String.format("total,%.2f\n", total.doubleValue()));

        ctx.contentType("text/csv; charset=utf-8")
           .result(csv.toString());
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid date format, expected yyyy-MM-dd");
        }
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month, MONTH_FORMAT);
        } catch (DateTimeParseException e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid month format, expected yyyy-MM");
        }
    }
}
