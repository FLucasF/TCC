package com.pousamarealta.services;

import com.pousamarealta.models.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ReportService {
    private ReservationService reservationService;
    private RoomService roomService = new RoomService();

    public ReportService(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public String getOccupancyReport(LocalDate date) {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        List<Reservation> occupiedRooms = new ArrayList<>();
        for (Reservation res : reservationService.getAllReservations()) {
            if (res.getStatus() != ReservationStatus.CANCELLED &&
                (res.getStatus() == ReservationStatus.CONFIRMED ||
                 res.getStatus() == ReservationStatus.CHECKED_IN ||
                 res.getStatus() == ReservationStatus.CHECKED_OUT)) {

                if (res.getCheckIn().isBefore(date.plusDays(1)) && res.getCheckOut().isAfter(date)) {
                    occupiedRooms.add(res);
                }
            }
        }

        occupiedRooms.sort((a, b) -> Integer.compare(a.getRoomNumber(), b.getRoomNumber()));

        for (Reservation res : occupiedRooms) {
            Room room = roomService.getRoomByNumber(res.getRoomNumber());
            csv.append(res.getRoomNumber()).append(",");
            csv.append(room.getType()).append(",");
            csv.append(res.getId()).append(",");
            csv.append(res.getCheckIn().format(DateTimeFormatter.ISO_DATE)).append(",");
            csv.append(res.getCheckOut().format(DateTimeFormatter.ISO_DATE)).append("\n");
        }

        csv.append("total,").append(occupiedRooms.size()).append("/5\n");

        return csv.toString();
    }

    public String getRevenueReport(YearMonth month) {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        List<Reservation> monthlyReservations = new ArrayList<>();
        for (Reservation res : reservationService.getAllReservations()) {
            if (YearMonth.from(res.getCheckIn()).equals(month) &&
                res.getStatus() != ReservationStatus.PENDING &&
                res.getStatus() != ReservationStatus.CANCELLED) {
                monthlyReservations.add(res);
            } else if (res.getStatus() == ReservationStatus.CANCELLED &&
                       YearMonth.from(res.getCheckIn()).equals(month) &&
                       res.getRatePlan() == RatePlan.NON_REFUNDABLE) {
                monthlyReservations.add(res);
            }
        }

        monthlyReservations.sort((a, b) -> {
            int dateCompare = a.getCheckIn().compareTo(b.getCheckIn());
            if (dateCompare != 0) return dateCompare;
            return Long.compare(a.getId(), b.getId());
        });

        BigDecimal total = BigDecimal.ZERO;
        for (Reservation res : monthlyReservations) {
            csv.append(res.getId()).append(",");
            csv.append(res.getRoomNumber()).append(",");
            csv.append(res.getCheckIn().format(DateTimeFormatter.ISO_DATE)).append(",");
            csv.append(res.getCheckOut().format(DateTimeFormatter.ISO_DATE)).append(",");
            csv.append(res.getStatus().getValue()).append(",");

            BigDecimal amount = BigDecimal.ZERO;
            if (res.getStatus() == ReservationStatus.CONFIRMED ||
                res.getStatus() == ReservationStatus.CHECKED_IN ||
                res.getStatus() == ReservationStatus.CHECKED_OUT) {
                amount = res.getAmountPaid();
            } else if (res.getStatus() == ReservationStatus.CANCELLED && res.getRatePlan() == RatePlan.NON_REFUNDABLE) {
                amount = res.getAmountPaid();
            }

            if (res.getRefund() != null && res.getRefund().compareTo(BigDecimal.ZERO) > 0) {
                amount = amount.subtract(res.getRefund());
            }

            csv.append(String.format("%.2f", amount)).append("\n");
            total = total.add(amount);
        }

        csv.append("total,,,,").append(String.format("%.2f", total)).append("\n");

        return csv.toString();
    }
}
