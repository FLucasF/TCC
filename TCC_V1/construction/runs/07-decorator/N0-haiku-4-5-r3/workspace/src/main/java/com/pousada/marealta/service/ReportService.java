package com.pousada.marealta.service;

import com.pousada.marealta.db.Database;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;

public class ReportService {
    public static String getOccupancyReport(LocalDate date) throws SQLException {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        int totalRooms = 5;
        int occupiedRooms = 0;

        try (Connection conn = Database.getConnection();
             var stmt = conn.createStatement()) {

            var result = stmt.executeQuery(
                    "SELECT r.id, r.room_number, rm.type, r.check_in, r.check_out " +
                    "FROM reservations r " +
                    "JOIN rooms rm ON r.room_number = rm.number " +
                    "WHERE r.status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') " +
                    "AND r.check_in <= '" + date + "' " +
                    "AND r.check_out > '" + date + "' " +
                    "ORDER BY r.room_number");

            while (result.next()) {
                csv.append(result.getInt("room_number")).append(",")
                        .append(result.getString("type")).append(",")
                        .append(result.getInt("id")).append(",")
                        .append(result.getDate("check_in")).append(",")
                        .append(result.getDate("check_out")).append("\n");
                occupiedRooms++;
            }
        }

        csv.append("total,").append(occupiedRooms).append("/").append(totalRooms);

        return csv.toString();
    }

    public static String getRevenueReport(YearMonth month) throws SQLException {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        java.math.BigDecimal totalRevenue = java.math.BigDecimal.ZERO;

        try (Connection conn = Database.getConnection();
             var stmt = conn.createStatement()) {

            String monthStr = String.format("%04d-%02d", month.getYear(), month.getMonthValue());

            var result = stmt.executeQuery(
                    "SELECT id, room_number, check_in, check_out, status, amount_paid, refund " +
                    "FROM reservations " +
                    "WHERE to_char(check_in, 'YYYY-MM') = '" + monthStr + "' " +
                    "AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED') " +
                    "AND (amount_paid > 0 OR refund > 0) " +
                    "ORDER BY check_in, id");

            while (result.next()) {
                int id = result.getInt("id");
                int room = result.getInt("room_number");
                var checkIn = result.getDate("check_in");
                var checkOut = result.getDate("check_out");
                String status = result.getString("status");
                var amountPaid = result.getBigDecimal("amount_paid");
                var refund = result.getBigDecimal("refund");

                var netAmount = amountPaid;
                if (refund != null && refund.signum() > 0) {
                    netAmount = amountPaid.subtract(refund);
                }

                csv.append(id).append(",")
                        .append(room).append(",")
                        .append(checkIn).append(",")
                        .append(checkOut).append(",")
                        .append(status).append(",")
                        .append(String.format("%.2f", netAmount)).append("\n");

                totalRevenue = totalRevenue.add(netAmount);
            }
        }

        csv.append("total,,,,").append(String.format("%.2f", totalRevenue));

        return csv.toString();
    }
}
