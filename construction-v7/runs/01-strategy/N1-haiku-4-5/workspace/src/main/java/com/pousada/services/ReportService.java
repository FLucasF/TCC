package com.pousada.services;

import com.pousada.models.Reservation;
import com.pousada.models.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ReportService {
    private final ReservationService reservationService;

    public ReportService(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public String getOccupancyReport(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return generateOccupancyReport(date);
        } catch (Exception e) {
            throw new ReservationService.ApiException("INVALID_REQUEST", "Formato de data inválido", 400);
        }
    }

    public String getRevenueReport(String monthStr) {
        try {
            String[] parts = monthStr.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            return generateRevenueReport(year, month);
        } catch (Exception e) {
            throw new ReservationService.ApiException("INVALID_REQUEST", "Formato de mês inválido", 400);
        }
    }

    private String generateOccupancyReport(LocalDate date) {
        StringBuilder sb = new StringBuilder();
        sb.append("room,type,reservation,check_in,check_out\n");

        int totalOccupied = 0;
        List<Room> rooms = new ArrayList<>(reservationService.getAllRooms());
        rooms.sort(Comparator.comparingInt(Room::getNumber));

        for (Room room : rooms) {
            List<Reservation> reservations = reservationService.getReservationsForRoom(room.getNumber(), date);
            for (Reservation reservation : reservations) {
                sb.append(room.getNumber()).append(",");
                sb.append(room.getType()).append(",");
                sb.append(reservation.getId()).append(",");
                sb.append(formatDate(reservation.getCheckIn())).append(",");
                sb.append(formatDate(reservation.getCheckOut())).append("\n");
                totalOccupied++;
            }
        }

        sb.append("total,").append(totalOccupied).append("/5\n");
        return sb.toString();
    }

    private String generateRevenueReport(int year, int month) {
        StringBuilder sb = new StringBuilder();
        sb.append("reservation,room,check_in,check_out,status,amount\n");

        List<Reservation> reservations = reservationService.getReservationsForMonth(year, month);
        reservations.sort((a, b) -> {
            int dateCompare = a.getCheckIn().compareTo(b.getCheckIn());
            if (dateCompare == 0) {
                return Long.compare(a.getId(), b.getId());
            }
            return dateCompare;
        });

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal amount = calculateRevenueAmount(reservation);
            sb.append(reservation.getId()).append(",");
            sb.append(reservation.getRoomNumber()).append(",");
            sb.append(formatDate(reservation.getCheckIn())).append(",");
            sb.append(formatDate(reservation.getCheckOut())).append(",");
            sb.append(reservation.getStatus()).append(",");
            sb.append(formatMoney(amount)).append("\n");
            totalAmount = totalAmount.add(amount);
        }

        sb.append("total,,,,").append(formatMoney(totalAmount)).append("\n");
        return sb.toString();
    }

    private BigDecimal calculateRevenueAmount(Reservation reservation) {
        if (reservation.getStatus() == Reservation.ReservationStatus.CANCELLED) {
            if (reservation.getRefund() != null) {
                return reservation.getAmountPaid().subtract(reservation.getRefund());
            }
            return BigDecimal.ZERO;
        }
        return reservation.getAmountPaid();
    }

    private String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private String formatMoney(BigDecimal value) {
        return String.format("%.2f", value);
    }
}
