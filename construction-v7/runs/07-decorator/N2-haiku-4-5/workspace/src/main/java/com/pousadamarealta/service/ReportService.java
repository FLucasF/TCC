package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;
import com.pousadamarealta.domain.Room;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.*;

public class ReportService {
    private final ReservationService reservationService;
    private final RoomService roomService;
    private static final DecimalFormat CURRENCY = new DecimalFormat("0.00");

    static {
        CURRENCY.setMinimumFractionDigits(2);
        CURRENCY.setMaximumFractionDigits(2);
    }

    public ReportService(ReservationService reservationService, RoomService roomService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
    }

    public String generateOccupancyReport(LocalDate date) {
        StringBuilder sb = new StringBuilder();
        sb.append("room,type,reservation,check_in,check_out\n");

        List<Room> rooms = new ArrayList<>(roomService.getAllRooms());
        rooms.sort(Comparator.comparingInt(Room::getNumber));

        int occupiedCount = 0;

        for (Room room : rooms) {
            List<Reservation> reservations = reservationService.getReservationsByRoom(room.getNumber(), date);

            for (Reservation res : reservations) {
                if (res.getStatus() == ReservationStatus.CONFIRMED ||
                    res.getStatus() == ReservationStatus.CHECKED_IN ||
                    res.getStatus() == ReservationStatus.CHECKED_OUT) {
                    sb.append(room.getNumber()).append(",")
                        .append(room.getType().getCode()).append(",")
                        .append(res.getId()).append(",")
                        .append(res.getCheckIn()).append(",")
                        .append(res.getCheckOut()).append("\n");
                    occupiedCount++;
                }
            }
        }

        sb.append("total,").append(occupiedCount).append("/").append(rooms.size()).append("\n");

        return sb.toString();
    }

    public String generateRevenueReport(int year, int month) {
        StringBuilder sb = new StringBuilder();
        sb.append("reservation,room,check_in,check_out,status,amount\n");

        List<Reservation> reservations = reservationService.getReservationsByMonth(year, month);
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Reservation res : reservations) {
            BigDecimal amount = res.getAmountPaid();
            if (res.getStatus() == ReservationStatus.CANCELLED && res.getRefund() != null) {
                amount = res.getAmountPaid().subtract(res.getRefund());
            }

            if (amount.signum() != 0) {
                sb.append(res.getId()).append(",")
                    .append(res.getRoomNumber()).append(",")
                    .append(res.getCheckIn()).append(",")
                    .append(res.getCheckOut()).append(",")
                    .append(res.getStatus().getCode()).append(",")
                    .append(formatAmount(amount)).append("\n");

                totalAmount = totalAmount.add(amount);
            }
        }

        sb.append("total,,,,").append(formatAmount(totalAmount)).append("\n");

        return sb.toString();
    }

    private String formatAmount(BigDecimal amount) {
        return amount.toPlainString();
    }
}
