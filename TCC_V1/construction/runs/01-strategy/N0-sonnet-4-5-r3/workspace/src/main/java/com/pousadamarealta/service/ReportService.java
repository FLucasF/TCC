package com.pousadamarealta.service;

import com.pousadamarealta.config.Database;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class ReportService {
    private final Database database;

    public ReportService(Database database) {
        this.database = database;
    }

    public String getOccupancyReport(LocalDate date) {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        String sql = """
            SELECT r.room_number, rm.type, r.id, r.check_in, r.check_out
            FROM reservations r
            JOIN rooms rm ON r.room_number = rm.number
            WHERE r.status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            AND r.check_in <= ?
            AND r.check_out > ?
            ORDER BY r.room_number
        """;

        int occupiedRooms = 0;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(date));
            stmt.setDate(2, Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    csv.append(rs.getString("room_number")).append(",");
                    csv.append(rs.getString("type")).append(",");
                    csv.append(rs.getLong("id")).append(",");
                    csv.append(rs.getDate("check_in").toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)).append(",");
                    csv.append(rs.getDate("check_out").toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\n");
                    occupiedRooms++;
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to generate occupancy report", e);
        }

        csv.append("total,").append(occupiedRooms).append("/5");

        return csv.toString();
    }

    public String getRevenueReport(YearMonth month) {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        String sql = """
            SELECT id, room_number, check_in, check_out, status, amount_paid, refund
            FROM reservations
            WHERE check_in >= ? AND check_in <= ?
            AND (amount_paid > 0 OR refund > 0)
            ORDER BY check_in, id
        """;

        BigDecimal totalRevenue = BigDecimal.ZERO;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    BigDecimal amountPaid = rs.getBigDecimal("amount_paid");
                    BigDecimal refund = rs.getBigDecimal("refund");
                    if (refund == null) {
                        refund = BigDecimal.ZERO;
                    }

                    BigDecimal netAmount = amountPaid.subtract(refund);

                    csv.append(rs.getLong("id")).append(",");
                    csv.append(rs.getString("room_number")).append(",");
                    csv.append(rs.getDate("check_in").toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)).append(",");
                    csv.append(rs.getDate("check_out").toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)).append(",");
                    csv.append(rs.getString("status")).append(",");
                    csv.append(formatCsvAmount(netAmount)).append("\n");

                    totalRevenue = totalRevenue.add(netAmount);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to generate revenue report", e);
        }

        csv.append("total,").append(formatCsvAmount(totalRevenue));

        return csv.toString();
    }

    private String formatCsvAmount(BigDecimal amount) {
        return amount.toString();
    }
}
