package com.pousada.service;

import com.pousada.db.Database;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;

public class ReportService {
    private final RoomService roomService = new RoomService();

    public String getOccupancyReport(LocalDate date) throws SQLException {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            String sql = "SELECT r.id, r.room_number, r.check_in, r.check_out, rm.type " +
                "FROM reservations r " +
                "JOIN rooms rm ON r.room_number = rm.number " +
                "WHERE r.status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') " +
                "AND r.check_in <= '" + date + "' " +
                "AND r.check_out > '" + date + "' " +
                "ORDER BY r.room_number ASC";
            var rs = stmt.executeQuery(sql);

            int occupiedCount = 0;
            while (rs.next()) {
                csv.append(rs.getInt("room_number")).append(",");
                csv.append(rs.getString("type")).append(",");
                csv.append(rs.getInt("id")).append(",");
                csv.append(rs.getDate("check_in")).append(",");
                csv.append(rs.getDate("check_out")).append("\n");
                occupiedCount++;
            }

            int totalRooms = roomService.getAllRooms().size();
            csv.append("total,").append(occupiedCount).append("/").append(totalRooms).append("\n");
        }

        return csv.toString();
    }

    public String getRevenueReport(YearMonth month) throws SQLException {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            String sql = "SELECT id, room_number, check_in, check_out, status, amount_paid, refund " +
                "FROM reservations " +
                "WHERE EXTRACT(YEAR FROM check_in) = " + month.getYear() + " " +
                "AND EXTRACT(MONTH FROM check_in) = " + month.getMonthValue() + " " +
                "AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED') " +
                "AND (status != 'CANCELLED' OR amount_paid > 0) " +
                "ORDER BY check_in ASC, id ASC";
            var rs = stmt.executeQuery(sql);

            BigDecimal totalAmount = BigDecimal.ZERO;
            while (rs.next()) {
                int id = rs.getInt("id");
                int room = rs.getInt("room_number");
                String checkIn = rs.getDate("check_in").toString();
                String checkOut = rs.getDate("check_out").toString();
                String status = rs.getString("status");
                BigDecimal amountPaid = rs.getBigDecimal("amount_paid");
                BigDecimal refund = rs.getBigDecimal("refund");

                BigDecimal netAmount = amountPaid;
                if (refund != null && refund.signum() > 0) {
                    netAmount = netAmount.subtract(refund);
                }

                csv.append(id).append(",");
                csv.append(room).append(",");
                csv.append(checkIn).append(",");
                csv.append(checkOut).append(",");
                csv.append(status).append(",");
                csv.append(formatDecimal(netAmount)).append("\n");

                totalAmount = totalAmount.add(netAmount);
            }

            csv.append("total,").append(formatDecimal(totalAmount)).append("\n");
        }

        return csv.toString();
    }

    private String formatDecimal(BigDecimal value) {
        return String.format("%.2f", value);
    }
}
