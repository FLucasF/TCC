package com.pousadamarealta;

import com.pousadamarealta.models.Room;
import com.pousadamarealta.models.Reservation;
import com.pousadamarealta.models.ReservationStatus;
import com.pousadamarealta.utils.DateParser;
import com.pousadamarealta.utils.PriceFormatter;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class ReportService {
    public static String generateOccupancyReport(String dateStr) {
        LocalDate date = DateParser.parse(dateStr);

        List<Room> allRooms = ReservationService.getRooms();
        List<Map<String, String>> occupiedRooms = new ArrayList<>();

        try (Connection conn = Database.getConnection()) {
            String sql = """
                SELECT r.id, r.room_number, room.type, r.check_in, r.check_out
                FROM reservations r
                JOIN rooms room ON r.room_number = room.number
                WHERE r.status != 'CANCELLED'
                AND r.check_in <= ?
                AND r.check_out > ?
                ORDER BY r.room_number, r.id
                """;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, date);
                stmt.setObject(2, date);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, String> row = new HashMap<>();
                        row.put("room", String.valueOf(rs.getInt("room_number")));
                        row.put("type", rs.getString("type"));
                        row.put("reservation", String.valueOf(rs.getLong("id")));
                        row.put("check_in", DateParser.format(rs.getObject("check_in", LocalDate.class)));
                        row.put("check_out", DateParser.format(rs.getObject("check_out", LocalDate.class)));
                        occupiedRooms.add(row);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");
        for (Map<String, String> row : occupiedRooms) {
            csv.append(row.get("room")).append(",");
            csv.append(row.get("type")).append(",");
            csv.append(row.get("reservation")).append(",");
            csv.append(row.get("check_in")).append(",");
            csv.append(row.get("check_out")).append("\n");
        }
        csv.append("total,").append(occupiedRooms.size()).append("/").append(allRooms.size()).append("\n");

        return csv.toString();
    }

    public static String generateRevenueReport(String monthStr) {
        YearMonth month;
        try {
            String[] parts = monthStr.split("-");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid month format. Expected yyyy-MM");
            }
            int year = Integer.parseInt(parts[0]);
            int monthNum = Integer.parseInt(parts[1]);
            month = YearMonth.of(year, monthNum);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid month format. Expected yyyy-MM");
        }

        List<Map<String, String>> revenues = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        try (Connection conn = Database.getConnection()) {
            String sql = """
                SELECT id, room_number, check_in, check_out, status, amount_paid, refund
                FROM reservations
                WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED')
                AND amount_paid > 0
                AND EXTRACT(YEAR FROM check_in) = ?
                AND EXTRACT(MONTH FROM check_in) = ?
                ORDER BY check_in, id
                """;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, month.getYear());
                stmt.setInt(2, month.getMonthValue());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        BigDecimal amountPaid = rs.getBigDecimal("amount_paid");
                        BigDecimal refund = rs.getBigDecimal("refund");
                        if (refund == null) refund = BigDecimal.ZERO;

                        BigDecimal net = amountPaid.subtract(refund);

                        Map<String, String> row = new HashMap<>();
                        row.put("reservation", String.valueOf(rs.getLong("id")));
                        row.put("room", String.valueOf(rs.getInt("room_number")));
                        row.put("check_in", DateParser.format(rs.getObject("check_in", LocalDate.class)));
                        row.put("check_out", DateParser.format(rs.getObject("check_out", LocalDate.class)));
                        row.put("status", rs.getString("status"));
                        row.put("amount", PriceFormatter.formatForCsv(net));
                        revenues.add(row);

                        total = total.add(net);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");
        for (Map<String, String> row : revenues) {
            csv.append(row.get("reservation")).append(",");
            csv.append(row.get("room")).append(",");
            csv.append(row.get("check_in")).append(",");
            csv.append(row.get("check_out")).append(",");
            csv.append(row.get("status")).append(",");
            csv.append(row.get("amount")).append("\n");
        }
        csv.append("total,").append(PriceFormatter.formatForCsv(total)).append("\n");

        return csv.toString();
    }
}
