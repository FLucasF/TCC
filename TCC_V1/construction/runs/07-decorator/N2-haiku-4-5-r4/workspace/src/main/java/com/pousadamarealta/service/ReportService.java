package com.pousadamarealta.service;

import com.pousadamarealta.db.Database;
import com.pousadamarealta.model.Room;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class ReportService {
    private final Database database;
    private final ReservationService reservationService;

    public ReportService(Database database, ReservationService reservationService) {
        this.database = database;
        this.reservationService = reservationService;
    }

    public String getOccupancyReport(LocalDate date) throws SQLException {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        List<Room> rooms = reservationService.getAllRooms();
        int occupiedCount = 0;

        try (Connection conn = database.getConnection()) {
            String sql = """
                SELECT id, room_number, check_in, check_out
                FROM reservations
                WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
                AND check_in <= ?
                AND check_out > ?
                ORDER BY room_number
                """;

            Map<Integer, String[]> occupiedRooms = new HashMap<>();

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setDate(1, java.sql.Date.valueOf(date));
                stmt.setDate(2, java.sql.Date.valueOf(date));
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    int roomNumber = rs.getInt("room_number");
                    int reservationId = rs.getInt("id");
                    LocalDate checkIn = rs.getDate("check_in").toLocalDate();
                    LocalDate checkOut = rs.getDate("check_out").toLocalDate();

                    Room room = rooms.stream()
                            .filter(r -> r.number == roomNumber)
                            .findFirst()
                            .orElse(null);

                    if (room != null) {
                        occupiedRooms.put(roomNumber, new String[]{
                            String.valueOf(roomNumber),
                            room.type,
                            String.valueOf(reservationId),
                            formatDate(checkIn),
                            formatDate(checkOut)
                        });
                        occupiedCount++;
                    }
                }
            }

            for (Room room : rooms) {
                if (occupiedRooms.containsKey(room.number)) {
                    String[] data = occupiedRooms.get(room.number);
                    csv.append(data[0]).append(",").append(data[1]).append(",")
                       .append(data[2]).append(",").append(data[3]).append(",")
                       .append(data[4]).append("\n");
                }
            }
        }

        csv.append("total,").append(occupiedCount).append("/").append(rooms.size());

        return csv.toString();
    }

    public String getRevenueReport(YearMonth month) throws SQLException {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        java.math.BigDecimal totalAmount = java.math.BigDecimal.ZERO;

        try (Connection conn = database.getConnection()) {
            String sql = """
                SELECT id, room_number, check_in, check_out, status, amount_paid, refund, rate_plan
                FROM reservations
                WHERE EXTRACT(YEAR FROM check_in) = ?
                AND EXTRACT(MONTH FROM check_in) = ?
                AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED')
                ORDER BY check_in, id
                """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, month.getYear());
                stmt.setInt(2, month.getMonthValue());
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    int reservationId = rs.getInt("id");
                    int roomNumber = rs.getInt("room_number");
                    LocalDate checkIn = rs.getDate("check_in").toLocalDate();
                    LocalDate checkOut = rs.getDate("check_out").toLocalDate();
                    String status = rs.getString("status");
                    java.math.BigDecimal amountPaid = rs.getBigDecimal("amount_paid");
                    java.math.BigDecimal refund = rs.getBigDecimal("refund");
                    String ratePlan = rs.getString("rate_plan");

                    java.math.BigDecimal amount = amountPaid;
                    if (refund != null && refund.compareTo(java.math.BigDecimal.ZERO) > 0) {
                        amount = amountPaid.subtract(refund);
                    }

                    csv.append(reservationId).append(",")
                       .append(roomNumber).append(",")
                       .append(formatDateCsv(checkIn)).append(",")
                       .append(formatDateCsv(checkOut)).append(",")
                       .append(status).append(",")
                       .append(formatMoneyCsv(amount)).append("\n");

                    totalAmount = totalAmount.add(amount);
                }
            }
        }

        csv.append("total,,,,").append(formatMoneyCsv(totalAmount));

        return csv.toString();
    }

    private String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private String formatDateCsv(LocalDate date) {
        return String.format("%04d-%02d-%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    private String formatMoneyCsv(java.math.BigDecimal amount) {
        return String.format("%.2f", amount);
    }
}
