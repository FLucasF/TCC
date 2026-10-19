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
import java.util.Locale;
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
            .toList();

        for (Reservation reservation : occupiedReservations) {
            Room room = roomService.getRoomByNumber(reservation.getRoomNumber()).orElseThrow();
            csv.append(String.format("%s,%s,%d,%s,%s\n",
                reservation.getRoomNumber(),
                room.getType(),
                reservation.getId(),
                reservation.getCheckIn().format(DateTimeFormatter.ISO_LOCAL_DATE),
                reservation.getCheckOut().format(DateTimeFormatter.ISO_LOCAL_DATE)
            ));
        }

        int totalRooms = roomService.getAllRooms().size();
        int occupiedRooms = occupiedReservations.size();
        csv.append(String.format("total,%d/%d\n", occupiedRooms, totalRooms));

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        List<Reservation> relevantReservations = reservationService.getAllReservations().stream()
            .filter(r -> YearMonth.from(r.getCheckIn()).equals(month))
            .filter(r -> r.getAmountPaid().compareTo(BigDecimal.ZERO) > 0 ||
                        (r.getRefund() != null && r.getRefund().compareTo(r.getAmountPaid()) < 0))
            .sorted(Comparator.comparing(Reservation::getCheckIn)
                .thenComparing(Reservation::getId))
            .toList();

        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Reservation reservation : relevantReservations) {
            BigDecimal revenue = reservation.getAmountPaid();
            if (reservation.getRefund() != null) {
                revenue = revenue.subtract(reservation.getRefund());
            }

            csv.append(String.format(Locale.US, "%d,%s,%s,%s,%s,%.2f\n",
                reservation.getId(),
                reservation.getRoomNumber(),
                reservation.getCheckIn().format(DateTimeFormatter.ISO_LOCAL_DATE),
                reservation.getCheckOut().format(DateTimeFormatter.ISO_LOCAL_DATE),
                reservation.getStatus(),
                revenue
            ));

            totalRevenue = totalRevenue.add(revenue);
        }

        csv.append(String.format(Locale.US, "total,%.2f\n", totalRevenue));

        return csv.toString();
    }
}
