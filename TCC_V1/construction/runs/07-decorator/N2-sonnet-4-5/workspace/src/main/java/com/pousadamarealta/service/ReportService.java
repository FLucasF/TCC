package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReportService {
    private final ReservationService reservationService;
    private final RoomService roomService;

    public ReportService(ReservationService reservationService, RoomService roomService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> occupiedReservations = reservationService.getReservationsForNight(date);
        int totalRooms = roomService.getAllRooms().size();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation r : occupiedReservations) {
            csv.append(r.roomNumber()).append(",")
               .append(r.roomType()).append(",")
               .append(r.id()).append(",")
               .append(r.checkIn()).append(",")
               .append(r.checkOut()).append("\n");
        }

        csv.append("total,").append(occupiedReservations.size()).append("/").append(totalRooms);
        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        List<Reservation> reservations = reservationService.getReservationsForMonth(year, month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation r : reservations) {
            BigDecimal amount = r.amountPaid();
            if (r.status() == ReservationStatus.CANCELLED && r.refund() != null) {
                amount = amount.subtract(r.refund());
            }

            csv.append(r.id()).append(",")
               .append(r.roomNumber()).append(",")
               .append(r.checkIn()).append(",")
               .append(r.checkOut()).append(",")
               .append(r.status()).append(",")
               .append(formatAmount(amount)).append("\n");

            total = total.add(amount);
        }

        csv.append("total,").append(formatAmount(total));
        return csv.toString();
    }

    private String formatAmount(BigDecimal amount) {
        return amount.toString().replace(",", "");
    }
}
