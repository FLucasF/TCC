package com.pousadamarealta.web.controller;

import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.service.RoomService;
import com.pousadamarealta.web.dto.ErrorResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

public class ReportController {
    private final ReservationService reservationService;
    private final RoomService roomService;
    private final int totalRooms;

    public ReportController(ReservationService reservationService, RoomService roomService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
        this.totalRooms = roomService.getAllRooms().size();
    }

    public void getOccupancyReport(Context ctx) {
        String dateStr = ctx.queryParam("date");

        if (dateStr == null) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("INVALID_REQUEST", "Missing date parameter"));
            return;
        }

        try {
            LocalDate date = LocalDate.parse(dateStr);
            List<Reservation> reservations = reservationService.getReservationsByDate(date);

            StringBuilder csv = new StringBuilder();
            csv.append("room,type,reservation,check_in,check_out\n");

            for (Reservation r : reservations) {
                csv.append(r.getRoomNumber()).append(",");
                csv.append(getRoomTypeName(r.getRoomNumber())).append(",");
                csv.append(r.getId()).append(",");
                csv.append(r.getCheckIn()).append(",");
                csv.append(r.getCheckOut()).append("\n");
            }

            csv.append("total,").append(reservations.size()).append("/").append(totalRooms);

            ctx.contentType("text/csv; charset=utf-8")
               .result(csv.toString());

        } catch (DateTimeParseException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        }
    }

    public void getRevenueReport(Context ctx) {
        String monthStr = ctx.queryParam("month");

        if (monthStr == null) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("INVALID_REQUEST", "Missing month parameter"));
            return;
        }

        try {
            String[] parts = monthStr.split("-");
            if (parts.length != 2) {
                throw new DateTimeParseException("Invalid format", monthStr, 0);
            }

            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);

            if (month < 1 || month > 12) {
                throw new DateTimeParseException("Invalid month", monthStr, 0);
            }

            List<Reservation> reservations = reservationService.getReservationsByMonth(year, month);

            StringBuilder csv = new StringBuilder();
            csv.append("reservation,room,check_in,check_out,status,amount\n");

            BigDecimal totalAmount = BigDecimal.ZERO;

            for (Reservation r : reservations) {
                BigDecimal netAmount = r.getAmountPaid();
                if (r.getRefund() != null) {
                    netAmount = netAmount.subtract(r.getRefund());
                }

                csv.append(r.getId()).append(",");
                csv.append(r.getRoomNumber()).append(",");
                csv.append(r.getCheckIn()).append(",");
                csv.append(r.getCheckOut()).append(",");
                csv.append(r.getStatus().name()).append(",");
                csv.append(String.format("%.2f", netAmount.doubleValue())).append("\n");

                totalAmount = totalAmount.add(netAmount);
            }

            csv.append("total,").append(String.format("%.2f", totalAmount.doubleValue()));

            ctx.contentType("text/csv; charset=utf-8")
               .result(csv.toString());

        } catch (DateTimeParseException | NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }

    private String getRoomTypeName(String roomNumber) {
        return switch (roomNumber) {
            case "101", "102" -> "STANDARD";
            case "201", "202" -> "DELUXE";
            case "301" -> "SUITE";
            default -> "";
        };
    }
}
