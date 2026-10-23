package com.pousadamarealta.service;

import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.ReservationStatus;
import com.pousadamarealta.model.Room;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

        List<Room> allRooms = roomService.getAllRooms();
        int occupiedCount = 0;

        for (Room room : allRooms) {
            List<Reservation> occupying = reservationService.findOccupyingRoom(room.getNumber(), date);

            if (!occupying.isEmpty()) {
                Reservation reservation = occupying.get(0);
                csv.append(String.format("%s,%s,%d,%s,%s\n",
                    room.getNumber(),
                    room.getType().name(),
                    reservation.getId(),
                    formatDate(reservation.getCheckIn()),
                    formatDate(reservation.getCheckOut())));
                occupiedCount++;
            }
        }

        csv.append(String.format("total,%d/%d\n", occupiedCount, allRooms.size()));

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        List<Reservation> reservations = reservationService.findByCheckInMonth(year, month);
        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal revenue = calculateRevenue(reservation);

            if (revenue.compareTo(BigDecimal.ZERO) > 0) {
                csv.append(String.format("%d,%s,%s,%s,%s,%s\n",
                    reservation.getId(),
                    reservation.getRoomNumber(),
                    formatDate(reservation.getCheckIn()),
                    formatDate(reservation.getCheckOut()),
                    reservation.getStatus().name(),
                    formatAmount(revenue)));

                totalRevenue = totalRevenue.add(revenue);
            }
        }

        csv.append(String.format("total,%s\n", formatAmount(totalRevenue)));

        return csv.toString();
    }

    private BigDecimal calculateRevenue(Reservation reservation) {
        BigDecimal paid = reservation.getAmountPaid();
        BigDecimal refunded = reservation.getRefund() != null ? reservation.getRefund() : BigDecimal.ZERO;
        return paid.subtract(refunded);
    }

    private String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private String formatAmount(BigDecimal amount) {
        return amount.toString();
    }
}
