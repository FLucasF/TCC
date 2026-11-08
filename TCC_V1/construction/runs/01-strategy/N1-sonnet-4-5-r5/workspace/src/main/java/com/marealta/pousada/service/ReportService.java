package com.marealta.pousada.service;

import com.marealta.pousada.domain.model.Reservation;
import com.marealta.pousada.domain.model.Room;

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
        List<Reservation> reservations = reservationService.getReservationsByNight(date);
        List<Room> allRooms = roomService.getAllRooms();

        StringBuilder sb = new StringBuilder();
        sb.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : reservations) {
            sb.append(reservation.getRoomNumber()).append(",");
            sb.append(reservation.getRoomType().name()).append(",");
            sb.append(reservation.getId()).append(",");
            sb.append(formatDateCsv(reservation.getCheckIn())).append(",");
            sb.append(formatDateCsv(reservation.getCheckOut())).append("\n");
        }

        sb.append("total,").append(reservations.size()).append("/").append(allRooms.size());

        return sb.toString();
    }

    public String generateRevenueReport(int year, int month) {
        List<Reservation> reservations = reservationService.getReservationsByMonth(year, month);

        StringBuilder sb = new StringBuilder();
        sb.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal amount = reservation.getRevenueAmount();

            sb.append(reservation.getId()).append(",");
            sb.append(reservation.getRoomNumber()).append(",");
            sb.append(formatDateCsv(reservation.getCheckIn())).append(",");
            sb.append(formatDateCsv(reservation.getCheckOut())).append(",");
            sb.append(reservation.getStatus().name()).append(",");
            sb.append(formatMoneyCsv(amount)).append("\n");

            total = total.add(amount);
        }

        sb.append("total,").append(formatMoneyCsv(total));

        return sb.toString();
    }

    private String formatDateCsv(LocalDate date) {
        return String.format("%04d-%02d-%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    private String formatMoneyCsv(BigDecimal amount) {
        return amount.toString();
    }
}
