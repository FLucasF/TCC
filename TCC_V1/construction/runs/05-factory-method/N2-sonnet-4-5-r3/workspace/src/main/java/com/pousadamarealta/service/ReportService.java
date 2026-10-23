package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.repository.ReservationRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReservationRepository reservationRepository;
    private final RoomService roomService;

    public ReportService(ReservationRepository reservationRepository, RoomService roomService) {
        this.reservationRepository = reservationRepository;
        this.roomService = roomService;
    }

    public String generateOccupancyReport(LocalDate date) throws SQLException {
        List<Room> allRooms = roomService.getAllRooms();
        StringBuilder csv = new StringBuilder();

        csv.append("room,type,reservation,check_in,check_out\n");

        int occupiedCount = 0;
        for (Room room : allRooms) {
            List<Reservation> reservations = reservationRepository.findByRoomAndDate(room.getNumber(), date);

            if (!reservations.isEmpty()) {
                for (Reservation reservation : reservations) {
                    csv.append(room.getNumber()).append(",")
                       .append(room.getType()).append(",")
                       .append(reservation.getId()).append(",")
                       .append(reservation.getCheckIn().format(DATE_FORMATTER)).append(",")
                       .append(reservation.getCheckOut().format(DATE_FORMATTER)).append("\n");
                }
                occupiedCount++;
            }
        }

        csv.append("total,").append(occupiedCount).append("/").append(allRooms.size());

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) throws SQLException {
        List<Reservation> reservations = reservationRepository.findByMonth(year, month);
        StringBuilder csv = new StringBuilder();

        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            Room room = roomService.getRoomByNumber(reservation.getRoomNumber())
                .orElseThrow(() -> new IllegalStateException("Room not found"));

            BigDecimal netAmount = reservation.getAmountPaid();
            if (reservation.getRefund() != null) {
                netAmount = netAmount.subtract(reservation.getRefund());
            }

            csv.append(reservation.getId()).append(",")
               .append(reservation.getRoomNumber()).append(",")
               .append(reservation.getCheckIn().format(DATE_FORMATTER)).append(",")
               .append(reservation.getCheckOut().format(DATE_FORMATTER)).append(",")
               .append(reservation.getStatus()).append(",")
               .append(String.format("%.2f", netAmount)).append("\n");

            total = total.add(netAmount);
        }

        csv.append("total,").append(String.format("%.2f", total));

        return csv.toString();
    }
}
