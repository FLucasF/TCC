package com.pousada.marealta.service;

import com.pousada.marealta.config.DatabaseConfig;
import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.repository.RoomRepository;

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

    public static String getOccupancyReport(LocalDate date) throws SQLException {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        String sql = "SELECT r.id, r.room_number, r.check_in, r.check_out " +
            "FROM reservations r " +
            "WHERE r.status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') " +
            "AND r.check_in <= ? " +
            "AND r.check_out > ? " +
            "ORDER BY r.room_number, r.id";

        int occupiedRooms = 0;
        try (Connection conn = DatabaseConfig.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setObject(1, date);
            stmt.setObject(2, date);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                long reservationId = rs.getLong("id");
                int roomNumber = rs.getInt("room_number");
                LocalDate checkIn = rs.getObject("check_in", LocalDate.class);
                LocalDate checkOut = rs.getObject("check_out", LocalDate.class);

                String roomType = RoomRepository.findByNumber(roomNumber).getType().name();
                csv.append(roomNumber).append(",")
                    .append(roomType).append(",")
                    .append(reservationId).append(",")
                    .append(checkIn).append(",")
                    .append(checkOut).append("\n");

                occupiedRooms++;
            }
        }

        csv.append("total,").append(occupiedRooms).append("/5\n");
        return csv.toString();
    }

    public static String getRevenueReport(YearMonth month) throws SQLException {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        String sql = "SELECT r.id, r.room_number, r.check_in, r.check_out, r.status, " +
            "r.amount_paid, r.refund " +
            "FROM reservations r " +
            "WHERE EXTRACT(YEAR FROM r.check_in) = ? " +
            "AND EXTRACT(MONTH FROM r.check_in) = ? " +
            "AND (r.amount_paid > 0 OR r.refund > 0) " +
            "AND r.status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED') " +
            "ORDER BY r.check_in, r.id";

        BigDecimal totalAmount = BigDecimal.ZERO;
        try (Connection conn = DatabaseConfig.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, month.getYear());
            stmt.setInt(2, month.getMonthValue());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                long reservationId = rs.getLong("id");
                int roomNumber = rs.getInt("room_number");
                LocalDate checkIn = rs.getObject("check_in", LocalDate.class);
                LocalDate checkOut = rs.getObject("check_out", LocalDate.class);
                String status = rs.getString("status");
                BigDecimal amountPaid = rs.getBigDecimal("amount_paid");
                BigDecimal refund = rs.getBigDecimal("refund");

                BigDecimal netAmount = amountPaid.subtract(refund == null ? BigDecimal.ZERO : refund);

                csv.append(reservationId).append(",")
                    .append(roomNumber).append(",")
                    .append(checkIn).append(",")
                    .append(checkOut).append(",")
                    .append(status).append(",")
                    .append(formatAmount(netAmount)).append("\n");

                totalAmount = totalAmount.add(netAmount);
            }
        }

        csv.append("total,").append(formatAmount(totalAmount)).append("\n");
        return csv.toString();
    }

    private static String formatAmount(BigDecimal amount) {
        return amount.toPlainString();
    }
}
