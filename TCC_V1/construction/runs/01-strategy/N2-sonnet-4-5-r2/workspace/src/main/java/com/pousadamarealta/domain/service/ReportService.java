package com.pousadamarealta.domain.service;

import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.domain.model.Room;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public class ReportService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReservationService reservationService;
    private final RoomService roomService;

    public ReportService(ReservationService reservationService, RoomService roomService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationService.findReservationsForNight(date);
        reservations.sort(Comparator.comparing(Reservation::getRoomNumber));

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : reservations) {
            Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
            csv.append(reservation.getRoomNumber()).append(",");
            csv.append(room.getType()).append(",");
            csv.append(reservation.getId()).append(",");
            csv.append(reservation.getCheckIn().format(DATE_FORMATTER)).append(",");
            csv.append(reservation.getCheckOut().format(DATE_FORMATTER)).append("\n");
        }

        int totalRooms = roomService.getAllRooms().size();
        csv.append("total,").append(reservations.size()).append("/").append(totalRooms);

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        List<Reservation> reservations = reservationService.findReservationsForMonth(year, month);

        reservations.sort(Comparator
            .comparing(Reservation::getCheckIn)
            .thenComparing(Reservation::getId));

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal netAmount = reservation.getAmountPaid();
            if (reservation.getRefund() != null) {
                netAmount = netAmount.subtract(reservation.getRefund());
            }

            if (netAmount.compareTo(BigDecimal.ZERO) > 0) {
                csv.append(reservation.getId()).append(",");
                csv.append(reservation.getRoomNumber()).append(",");
                csv.append(reservation.getCheckIn().format(DATE_FORMATTER)).append(",");
                csv.append(reservation.getCheckOut().format(DATE_FORMATTER)).append(",");
                csv.append(reservation.getStatus()).append(",");
                csv.append(formatAmount(netAmount)).append("\n");

                totalRevenue = totalRevenue.add(netAmount);
            }
        }

        csv.append("total,").append(formatAmount(totalRevenue));

        return csv.toString();
    }

    private String formatAmount(BigDecimal amount) {
        return amount.toString();
    }
}
