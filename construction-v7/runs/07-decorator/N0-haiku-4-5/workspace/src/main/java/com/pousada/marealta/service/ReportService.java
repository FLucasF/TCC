package com.pousada.marealta.service;

import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.model.ReservationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

public class ReportService {
    private final ReservationService reservationService;
    private final RoomManager roomManager;

    public ReportService(ReservationService reservationService, RoomManager roomManager) {
        this.reservationService = reservationService;
        this.roomManager = roomManager;
    }

    public String generateOccupancyReport(LocalDate date) {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        List<Reservation> occupiedReservations = reservationService.getAllReservations().stream()
            .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED ||
                        r.getStatus() == ReservationStatus.CHECKED_IN ||
                        r.getStatus() == ReservationStatus.CHECKED_OUT)
            .filter(r -> !date.isBefore(r.getCheckIn()) && date.isBefore(r.getCheckOut()))
            .sorted(Comparator.comparingInt(Reservation::getRoomNumber))
            .toList();

        for (Reservation r : occupiedReservations) {
            var room = roomManager.getRoom(r.getRoomNumber()).orElseThrow();
            csv.append(r.getRoomNumber()).append(",");
            csv.append(room.type()).append(",");
            csv.append(r.getId()).append(",");
            csv.append(r.getCheckIn()).append(",");
            csv.append(r.getCheckOut()).append("\n");
        }

        int totalRooms = (int) roomManager.getAllRooms().stream().count();
        csv.append("total,").append(occupiedReservations.size()).append("/").append(totalRooms).append("\n");

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth yearMonth) {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        List<Reservation> monthReservations = reservationService.getAllReservations().stream()
            .filter(r -> YearMonth.from(r.getCheckIn()).equals(yearMonth))
            .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED ||
                        r.getStatus() == ReservationStatus.CHECKED_IN ||
                        r.getStatus() == ReservationStatus.CHECKED_OUT ||
                        r.getStatus() == ReservationStatus.CANCELLED)
            .filter(r -> r.getAmountPaid().compareTo(BigDecimal.ZERO) > 0 ||
                        (r.getRefund() != null && r.getRefund().compareTo(BigDecimal.ZERO) > 0))
            .sorted(Comparator.comparing(Reservation::getCheckIn)
                .thenComparingLong(Reservation::getId))
            .toList();

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Reservation r : monthReservations) {
            BigDecimal amount = r.getAmountPaid();
            if (r.getRefund() != null) {
                amount = amount.subtract(r.getRefund());
            }

            csv.append(r.getId()).append(",");
            csv.append(r.getRoomNumber()).append(",");
            csv.append(r.getCheckIn()).append(",");
            csv.append(r.getCheckOut()).append(",");
            csv.append(r.getStatus()).append(",");
            csv.append(String.format("%.2f", amount)).append("\n");

            totalAmount = totalAmount.add(amount);
        }

        csv.append("total,").append(String.format("%.2f", totalAmount)).append("\n");

        return csv.toString();
    }
}
