package com.pousadamarealta.service;

import com.pousadamarealta.database.Database;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReportService {
    public static String getOccupancyReport(LocalDate date) throws SQLException {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement(
                "SELECT r.number, r.type, res.id, res.check_in, res.check_out " +
                "FROM rooms r " +
                "LEFT JOIN reservations res ON r.number = res.room_number " +
                "AND res.status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') " +
                "AND res.check_in <= ? " +
                "AND res.check_out > ? " +
                "ORDER BY r.number");
            stmt.setObject(1, date);
            stmt.setObject(2, date);

            var rs = stmt.executeQuery();

            int occupied = 0;
            while (rs.next()) {
                int roomNumber = rs.getInt("number");
                String type = rs.getString("type");
                Object reservationId = rs.getObject("id");

                if (reservationId != null) {
                    String checkIn = rs.getObject("check_in").toString();
                    String checkOut = rs.getObject("check_out").toString();
                    csv.append(roomNumber).append(",").append(type).append(",")
                       .append(reservationId).append(",").append(checkIn).append(",")
                       .append(checkOut).append("\n");
                    occupied++;
                }
            }

            csv.append("total,").append(occupied).append("/5\n");
        }

        return csv.toString();
    }

    public static String getRevenueReport(YearMonth month) throws SQLException {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        java.math.BigDecimal totalRevenue = java.math.BigDecimal.ZERO;

        try (Connection conn = Database.getConnection()) {
            LocalDate monthStart = month.atDay(1);
            LocalDate monthEnd = month.atEndOfMonth();

            var stmt = conn.prepareStatement(
                "SELECT id, room_number, check_in, check_out, status, amount_paid, refund " +
                "FROM reservations " +
                "WHERE check_in >= ? " +
                "AND check_in <= ? " +
                "AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED') " +
                "AND (amount_paid > 0 OR refund > 0) " +
                "ORDER BY check_in, id");
            stmt.setObject(1, monthStart);
            stmt.setObject(2, monthEnd);

            var rs = stmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                int room = rs.getInt("room_number");
                String checkIn = rs.getObject("check_in").toString();
                String checkOut = rs.getObject("check_out").toString();
                String status = rs.getString("status");
                java.math.BigDecimal amountPaid = rs.getBigDecimal("amount_paid");
                java.math.BigDecimal refund = rs.getBigDecimal("refund");

                java.math.BigDecimal net = amountPaid;
                if (refund != null) {
                    net = net.subtract(refund);
                }

                if (net.compareTo(java.math.BigDecimal.ZERO) != 0) {
                    csv.append(id).append(",").append(room).append(",")
                       .append(checkIn).append(",").append(checkOut).append(",")
                       .append(status).append(",").append(net.toPlainString()).append("\n");

                    totalRevenue = totalRevenue.add(net);
                }
            }

            csv.append("total,").append(totalRevenue.toPlainString()).append("\n");
        }

        return csv.toString();
    }
}
