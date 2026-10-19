package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;
import com.pousadamarealta.domain.Room;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ReportService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final RoomService roomService;
    private final ReservationService reservationService;

    public ReportService(RoomService roomService, ReservationService reservationService) {
        this.roomService = roomService;
        this.reservationService = reservationService;
    }

    public String generateOccupancyReport(LocalDate date) {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        List<Reservation> occupiedReservations = reservationService.getAllReservations().stream()
            .filter(r -> r.getStatus() != ReservationStatus.PENDING &&
                        r.getStatus() != ReservationStatus.CANCELLED)
            .filter(r -> r.occupiesNight(date))
            .sorted(Comparator.comparing(Reservation::getRoomNumber))
            .toList();

        for (Reservation reservation : occupiedReservations) {
            Room room = roomService.getRoomByNumber(reservation.getRoomNumber()).orElseThrow();
            csv.append(reservation.getRoomNumber()).append(",")
               .append(room.getType()).append(",")
               .append(reservation.getId()).append(",")
               .append(DATE_FORMAT.format(reservation.getCheckIn())).append(",")
               .append(DATE_FORMAT.format(reservation.getCheckOut())).append("\n");
        }

        int totalRooms = roomService.getAllRooms().size();
        csv.append("total,").append(occupiedReservations.size()).append("/").append(totalRooms);

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();

        List<Reservation> monthReservations = reservationService.getAllReservations().stream()
            .filter(r -> !r.getCheckIn().isBefore(monthStart) && !r.getCheckIn().isAfter(monthEnd))
            .filter(r -> {
                BigDecimal revenue = r.getAmountPaid();
                if (r.getRefund() != null) {
                    revenue = revenue.subtract(r.getRefund());
                }
                return revenue.compareTo(BigDecimal.ZERO) > 0;
            })
            .sorted(Comparator.comparing(Reservation::getCheckIn)
                             .thenComparing(Reservation::getId))
            .toList();

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation reservation : monthReservations) {
            BigDecimal amount = reservation.getAmountPaid();
            if (reservation.getRefund() != null) {
                amount = amount.subtract(reservation.getRefund());
            }
            total = total.add(amount);

            csv.append(reservation.getId()).append(",")
               .append(reservation.getRoomNumber()).append(",")
               .append(DATE_FORMAT.format(reservation.getCheckIn())).append(",")
               .append(DATE_FORMAT.format(reservation.getCheckOut())).append(",")
               .append(reservation.getStatus()).append(",")
               .append(formatAmount(amount)).append("\n");
        }

        csv.append("total,").append(formatAmount(total));

        return csv.toString();
    }

    private String formatAmount(BigDecimal amount) {
        return amount.toString();
    }
}
