package com.pousadamarealta.service;

import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.ReservationStatus;
import com.pousadamarealta.model.Room;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ReportService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReservationService reservationService;

    public ReportService(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public String generateOccupancyReport(LocalDate date) {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        List<Reservation> occupiedReservations = reservationService.getAllReservations().stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED ||
                           r.getStatus() == ReservationStatus.CHECKED_IN ||
                           r.getStatus() == ReservationStatus.CHECKED_OUT)
                .filter(r -> !r.getCheckIn().isAfter(date) && r.getCheckOut().isAfter(date))
                .sorted(Comparator.comparing(Reservation::getRoomNumber))
                .collect(Collectors.toList());

        for (Reservation reservation : occupiedReservations) {
            Room room = reservationService.getRoom(reservation.getRoomNumber());
            csv.append(String.format("%s,%s,%d,%s,%s\n",
                    reservation.getRoomNumber(),
                    room.getType(),
                    reservation.getId(),
                    reservation.getCheckIn().format(DATE_FORMAT),
                    reservation.getCheckOut().format(DATE_FORMAT)));
        }

        int totalRooms = reservationService.getAllRooms().size();
        int occupiedRooms = occupiedReservations.size();
        csv.append(String.format("total,%d/%d", occupiedRooms, totalRooms));

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        List<Reservation> relevantReservations = reservationService.getAllReservations().stream()
                .filter(r -> YearMonth.from(r.getCheckIn()).equals(month))
                .filter(r -> {
                    BigDecimal revenue = calculateRevenue(r);
                    return revenue.compareTo(BigDecimal.ZERO) > 0;
                })
                .sorted(Comparator.comparing(Reservation::getCheckIn)
                        .thenComparing(Reservation::getId))
                .collect(Collectors.toList());

        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Reservation reservation : relevantReservations) {
            BigDecimal revenue = calculateRevenue(reservation);
            csv.append(String.format("%d,%s,%s,%s,%s,%s\n",
                    reservation.getId(),
                    reservation.getRoomNumber(),
                    reservation.getCheckIn().format(DATE_FORMAT),
                    reservation.getCheckOut().format(DATE_FORMAT),
                    reservation.getStatus(),
                    formatAmount(revenue)));
            totalRevenue = totalRevenue.add(revenue);
        }

        csv.append(String.format("total,%s", formatAmount(totalRevenue)));

        return csv.toString();
    }

    private BigDecimal calculateRevenue(Reservation reservation) {
        BigDecimal revenue = reservation.getAmountPaid();
        if (reservation.getRefund() != null) {
            revenue = revenue.subtract(reservation.getRefund());
        }
        return revenue;
    }

    private String formatAmount(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }
}
