package com.pousada.service;

import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;
import com.pousada.domain.Room;

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
    private static final DateTimeFormatter CSV_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ReportService(ReservationService reservationService, RoomService roomService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> occupiedReservations = reservationService.getAllReservations().stream()
            .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED ||
                        r.getStatus() == ReservationStatus.CHECKED_IN ||
                        r.getStatus() == ReservationStatus.CHECKED_OUT)
            .filter(r -> occupiesNight(r, date))
            .sorted(Comparator.comparing(Reservation::getRoomNumber))
            .collect(Collectors.toList());

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation r : occupiedReservations) {
            Room room = roomService.getRoom(r.getRoomNumber());
            csv.append(r.getRoomNumber()).append(",")
               .append(room.getType()).append(",")
               .append(r.getId()).append(",")
               .append(r.getCheckIn().format(CSV_DATE_FORMAT)).append(",")
               .append(r.getCheckOut().format(CSV_DATE_FORMAT)).append("\n");
        }

        int totalRooms = roomService.getAllRooms().size();
        csv.append("total,").append(occupiedReservations.size()).append("/").append(totalRooms);

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> relevantReservations = reservationService.getAllReservations().stream()
            .filter(r -> YearMonth.from(r.getCheckIn()).equals(month))
            .filter(r -> getRevenue(r).compareTo(BigDecimal.ZERO) > 0)
            .sorted(Comparator.comparing(Reservation::getCheckIn)
                             .thenComparing(Reservation::getId))
            .collect(Collectors.toList());

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation r : relevantReservations) {
            BigDecimal revenue = getRevenue(r);
            csv.append(r.getId()).append(",")
               .append(r.getRoomNumber()).append(",")
               .append(r.getCheckIn().format(CSV_DATE_FORMAT)).append(",")
               .append(r.getCheckOut().format(CSV_DATE_FORMAT)).append(",")
               .append(r.getStatus()).append(",")
               .append(formatCsvAmount(revenue)).append("\n");
            total = total.add(revenue);
        }

        csv.append("total,").append(formatCsvAmount(total));

        return csv.toString();
    }

    private boolean occupiesNight(Reservation reservation, LocalDate night) {
        return !reservation.getCheckIn().isAfter(night) &&
               reservation.getCheckOut().isAfter(night);
    }

    private BigDecimal getRevenue(Reservation reservation) {
        BigDecimal paid = reservation.getAmountPaid();
        BigDecimal refunded = reservation.getRefund() != null ?
                             reservation.getRefund() : BigDecimal.ZERO;
        return paid.subtract(refunded);
    }

    private String formatCsvAmount(BigDecimal amount) {
        return amount.toString();
    }
}
