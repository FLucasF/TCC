package com.mareaalta.service;

import com.mareaalta.domain.Reservation;
import com.mareaalta.domain.Room;
import com.mareaalta.infrastructure.ReservationRepository;
import com.mareaalta.infrastructure.RoomRepository;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ReportService {
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String generateOccupancyReport(LocalDate date) throws SQLException {
        List<Room> allRooms = RoomRepository.findAll();
        Map<Integer, Reservation> occupiedRooms = new HashMap<>();

        for (Room room : allRooms) {
            List<Reservation> reservations = ReservationRepository.findByRoomAndDate(room.getNumber(), date);
            if (!reservations.isEmpty()) {
                occupiedRooms.put(room.getNumber(), reservations.get(0));
            }
        }

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Room room : allRooms) {
            if (occupiedRooms.containsKey(room.getNumber())) {
                Reservation res = occupiedRooms.get(room.getNumber());
                csv.append(room.getNumber()).append(",")
                   .append(room.getType()).append(",")
                   .append(res.getId()).append(",")
                   .append(res.getCheckIn().format(ISO_DATE)).append(",")
                   .append(res.getCheckOut().format(ISO_DATE)).append("\n");
            }
        }

        csv.append("total,").append(occupiedRooms.size()).append("/").append(allRooms.size()).append("\n");

        return csv.toString();
    }

    public static String generateRevenueReport(int year, int month) throws SQLException {
        List<Reservation> reservations = ReservationRepository.findByMonthStarting(year, month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation res : reservations) {
            BigDecimal amount = calculateRevenueAmount(res);
            total = total.add(amount);

            csv.append(res.getId()).append(",")
               .append(res.getRoomNumber()).append(",")
               .append(res.getCheckIn().format(ISO_DATE)).append(",")
               .append(res.getCheckOut().format(ISO_DATE)).append(",")
               .append(res.getStatus()).append(",")
               .append(formatDecimal(amount)).append("\n");
        }

        csv.append("total,,,,").append(formatDecimal(total)).append("\n");

        return csv.toString();
    }

    private static BigDecimal calculateRevenueAmount(Reservation reservation) {
        if (reservation.getStatus() == Reservation.Status.CANCELLED) {
            if (reservation.getRefund() == null) {
                return BigDecimal.ZERO;
            }
            return reservation.getAmountPaid().subtract(reservation.getRefund());
        }

        if (reservation.getStatus() == Reservation.Status.PENDING) {
            return BigDecimal.ZERO;
        }

        return reservation.getAmountPaid();
    }

    private static String formatDecimal(BigDecimal value) {
        return String.format("%.2f", value);
    }
}
