package com.pousada.service;

import com.pousada.model.Reservation;

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
        List<Reservation> occupiedRooms = reservationService.getAllReservations().stream()
            .filter(r -> r.isPaid())
            .filter(r -> !r.getCheckIn().isAfter(date) && r.getCheckOut().isAfter(date))
            .sorted(Comparator.comparing(Reservation::getRoomNumber))
            .collect(Collectors.toList());

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Reservation reservation : occupiedRooms) {
            csv.append(String.format("%s,%s,%d,%s,%s\n",
                reservation.getRoomNumber(),
                roomService.getRoom(reservation.getRoomNumber()).getType(),
                reservation.getId(),
                reservation.getCheckIn().format(formatter),
                reservation.getCheckOut().format(formatter)
            ));
        }

        csv.append(String.format("total,%d/%d\n", occupiedRooms.size(), roomService.getTotalRooms()));

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> monthReservations = reservationService.getAllReservations().stream()
            .filter(r -> {
                YearMonth checkInMonth = YearMonth.from(r.getCheckIn());
                return checkInMonth.equals(month);
            })
            .filter(r -> {
                BigDecimal revenue = r.getAmountPaid();
                if (r.getRefund() != null) {
                    revenue = revenue.subtract(r.getRefund());
                }
                return revenue.compareTo(BigDecimal.ZERO) > 0;
            })
            .sorted(Comparator.comparing(Reservation::getCheckIn)
                .thenComparing(Reservation::getId))
            .collect(Collectors.toList());

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        BigDecimal total = BigDecimal.ZERO;

        for (Reservation reservation : monthReservations) {
            BigDecimal amount = reservation.getAmountPaid();
            if (reservation.getRefund() != null) {
                amount = amount.subtract(reservation.getRefund());
            }

            csv.append(String.format("%d,%s,%s,%s,%s,%s\n",
                reservation.getId(),
                reservation.getRoomNumber(),
                reservation.getCheckIn().format(formatter),
                reservation.getCheckOut().format(formatter),
                reservation.getStatus(),
                formatAmount(amount)
            ));

            total = total.add(amount);
        }

        csv.append(String.format("total,%s\n", formatAmount(total)));

        return csv.toString();
    }

    private String formatAmount(BigDecimal amount) {
        return amount.toString();
    }
}
