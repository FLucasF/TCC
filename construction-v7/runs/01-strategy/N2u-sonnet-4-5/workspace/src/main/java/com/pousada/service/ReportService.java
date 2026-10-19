package com.pousada.service;

import com.pousada.domain.Reservation;
import com.pousada.domain.Room;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ReportService {
    private final ReservationService reservationService;
    private final RoomService roomService;

    public ReportService(ReservationService reservationService, RoomService roomService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> occupiedReservations = reservationService.getAllReservations().stream()
            .filter(Reservation::isPaid)
            .filter(r -> isOccupiedOnNight(r, date))
            .sorted(Comparator.comparing(Reservation::getRoomNumber))
            .toList();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : occupiedReservations) {
            Room room = roomService.findByNumber(reservation.getRoomNumber()).orElseThrow();
            csv.append(reservation.getRoomNumber()).append(",");
            csv.append(room.getType()).append(",");
            csv.append(reservation.getId()).append(",");
            csv.append(reservation.getCheckIn()).append(",");
            csv.append(reservation.getCheckOut()).append("\n");
        }

        int totalRooms = roomService.getAllRooms().size();
        int occupiedRooms = occupiedReservations.size();
        csv.append("total,").append(occupiedRooms).append("/").append(totalRooms);

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> monthReservations = reservationService.getAllReservations().stream()
            .filter(r -> YearMonth.from(r.getCheckIn()).equals(month))
            .filter(this::hasRevenue)
            .sorted(Comparator.comparing(Reservation::getCheckIn)
                .thenComparing(Reservation::getId))
            .toList();

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Reservation reservation : monthReservations) {
            BigDecimal revenue = calculateRevenue(reservation);
            totalRevenue = totalRevenue.add(revenue);

            csv.append(reservation.getId()).append(",");
            csv.append(reservation.getRoomNumber()).append(",");
            csv.append(reservation.getCheckIn()).append(",");
            csv.append(reservation.getCheckOut()).append(",");
            csv.append(reservation.getStatus()).append(",");
            csv.append(formatCsvMoney(revenue)).append("\n");
        }

        csv.append("total,").append(formatCsvMoney(totalRevenue));

        return csv.toString();
    }

    private boolean isOccupiedOnNight(Reservation reservation, LocalDate night) {
        return !night.isBefore(reservation.getCheckIn()) && night.isBefore(reservation.getCheckOut());
    }

    private boolean hasRevenue(Reservation reservation) {
        BigDecimal revenue = calculateRevenue(reservation);
        return revenue.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal calculateRevenue(Reservation reservation) {
        BigDecimal paid = reservation.getAmountPaid();
        BigDecimal refunded = reservation.getRefund() != null ? reservation.getRefund() : BigDecimal.ZERO;
        return paid.subtract(refunded);
    }

    private String formatCsvMoney(BigDecimal amount) {
        return amount.toString();
    }
}
