package com.pousadamareal.service;

import com.pousadamareal.model.Reservation;
import com.pousadamareal.model.ReservationStatus;
import com.pousadamareal.model.Room;
import com.pousadamareal.repository.ReservationRepository;
import com.pousadamareal.repository.RoomRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.HashMap;
import java.util.Map;

public class ReportService {

    public static String generateOccupancyReport(LocalDate date) throws SQLException {
        List<Reservation> reservations = ReservationRepository.findByDateForOccupancy(date);
        List<Room> allRooms = RoomRepository.findAll();

        Map<Integer, Room> roomMap = new HashMap<>();
        for (Room room : allRooms) {
            roomMap.put(room.number(), room);
        }

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation res : reservations) {
            Room room = roomMap.get(res.roomNumber());
            String roomType = room != null ? room.type().name() : "UNKNOWN";
            csv.append(res.roomNumber()).append(",")
               .append(roomType).append(",")
               .append(res.id()).append(",")
               .append(formatDate(res.checkIn())).append(",")
               .append(formatDate(res.checkOut())).append("\n");
        }

        csv.append("total,").append(reservations.size()).append("/").append(allRooms.size());

        return csv.toString();
    }

    public static String generateRevenueReport(int year, int month) throws SQLException {
        List<Reservation> reservations = ReservationRepository.findByMonthForRevenue(year, month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation res : reservations) {
            BigDecimal amount = res.amountPaid();

            if (res.status() == ReservationStatus.CANCELLED) {
                if (res.refund() != null) {
                    amount = res.amountPaid().subtract(res.refund());
                } else {
                    amount = res.amountPaid();
                }
            }

            csv.append(res.id()).append(",")
               .append(res.roomNumber()).append(",")
               .append(formatDate(res.checkIn())).append(",")
               .append(formatDate(res.checkOut())).append(",")
               .append(res.status().name()).append(",")
               .append(formatMoney(amount)).append("\n");

            total = total.add(amount);
        }

        csv.append("total,,,,").append(formatMoney(total));

        return csv.toString();
    }

    private static String formatDate(LocalDate date) {
        return String.format("%d-%02d-%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    private static String formatMoney(BigDecimal value) {
        return String.format("%.2f", value);
    }
}
