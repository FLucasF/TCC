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
    private final ReservationService reservationService;
    private final RoomService roomService;

    public ReportService(ReservationService reservationService, RoomService roomService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
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
            Room room = roomService.getRoom(reservation.getRoomNumber());
            csv.append(reservation.getRoomNumber()).append(",");
            csv.append(room.getType()).append(",");
            csv.append(reservation.getId()).append(",");
            csv.append(formatDateForCsv(reservation.getCheckIn())).append(",");
            csv.append(formatDateForCsv(reservation.getCheckOut())).append("\n");
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
                BigDecimal netAmount = r.getAmountPaid().subtract(
                    r.getRefund() != null ? r.getRefund() : BigDecimal.ZERO
                );
                return netAmount.compareTo(BigDecimal.ZERO) > 0;
            })
            .sorted(Comparator.comparing(Reservation::getCheckIn)
                             .thenComparing(Reservation::getId))
            .collect(Collectors.toList());

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation reservation : monthReservations) {
            BigDecimal netAmount = reservation.getAmountPaid().subtract(
                reservation.getRefund() != null ? reservation.getRefund() : BigDecimal.ZERO
            );

            csv.append(reservation.getId()).append(",");
            csv.append(reservation.getRoomNumber()).append(",");
            csv.append(formatDateForCsv(reservation.getCheckIn())).append(",");
            csv.append(formatDateForCsv(reservation.getCheckOut())).append(",");
            csv.append(reservation.getStatus()).append(",");
            csv.append(formatMoneyForCsv(netAmount)).append("\n");

            total = total.add(netAmount);
        }

        csv.append("total,").append(formatMoneyForCsv(total));

        return csv.toString();
    }

    private String formatDateForCsv(LocalDate date) {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private String formatMoneyForCsv(BigDecimal value) {
        return String.format("%.2f", value);
    }
}
