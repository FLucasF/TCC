package com.marealta.service;

import com.marealta.database.Database;
import com.marealta.domain.Reservation;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class ReportService {
    public static String generateOccupancyReport(LocalDate date) throws SQLException {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        List<Integer> roomNumbers = getRoomNumbers();
        int occupiedCount = 0;

        for (int roomNumber : roomNumbers) {
            List<Reservation> reservations = ReservationService.getReservationsByRoomAndDate(roomNumber, date);

            if (!reservations.isEmpty()) {
                for (Reservation res : reservations) {
                    csv.append(res.roomNumber).append(",")
                       .append(getRoomType(roomNumber)).append(",")
                       .append(res.id).append(",")
                       .append(res.checkIn).append(",")
                       .append(res.checkOut).append("\n");
                }
                occupiedCount++;
            }
        }

        csv.append("total,").append(occupiedCount).append("/5\n");
        return csv.toString();
    }

    public static String generateRevenueReport(YearMonth month) throws SQLException {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        String sql = """
            SELECT r.id, rm.number, r.check_in, r.check_out, r.status,
                   r.amount_paid, r.refund, r.total
            FROM reservations r
            JOIN rooms rm ON r.room_id = rm.id
            WHERE EXTRACT(YEAR FROM r.check_in) = ? AND EXTRACT(MONTH FROM r.check_in) = ?
              AND (r.status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') OR
                   (r.status = 'CANCELLED' AND r.amount_paid > 0))
            ORDER BY r.check_in, r.id
        """;

        BigDecimal total = BigDecimal.ZERO;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, month.getYear());
            stmt.setInt(2, month.getMonthValue());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int reservationId = rs.getInt("id");
                    int roomNumber = rs.getInt("number");
                    LocalDate checkIn = rs.getObject("check_in", LocalDate.class);
                    LocalDate checkOut = rs.getObject("check_out", LocalDate.class);
                    String status = rs.getString("status");
                    BigDecimal amountPaid = rs.getBigDecimal("amount_paid");
                    BigDecimal refund = rs.getBigDecimal("refund");

                    BigDecimal amount;
                    if (status.equals("CANCELLED")) {
                        amount = amountPaid;
                        if (refund != null && refund.compareTo(BigDecimal.ZERO) > 0) {
                            amount = amount.subtract(refund);
                        }
                    } else {
                        amount = amountPaid;
                        if (refund != null && refund.compareTo(BigDecimal.ZERO) > 0) {
                            amount = amount.subtract(refund);
                        }
                    }

                    csv.append(reservationId).append(",")
                       .append(roomNumber).append(",")
                       .append(checkIn).append(",")
                       .append(checkOut).append(",")
                       .append(status).append(",")
                       .append(String.format("%.2f", amount)).append("\n");

                    total = total.add(amount);
                }
            }
        }

        csv.append("total,,,,").append(String.format("%.2f", total)).append("\n");
        return csv.toString();
    }

    private static List<Integer> getRoomNumbers() throws SQLException {
        List<Integer> roomNumbers = new ArrayList<>();
        String sql = "SELECT number FROM rooms ORDER BY number";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                roomNumbers.add(rs.getInt("number"));
            }
        }
        return roomNumbers;
    }

    private static String getRoomType(int roomNumber) throws SQLException {
        String sql = "SELECT type FROM rooms WHERE number = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roomNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("type");
                }
            }
        }
        return "";
    }
}
