package com.pousadamarealta;

import io.javalin.Javalin;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class ReportController {
    private final Database database;

    public ReportController(Database database) {
        this.database = database;
    }

    public void register(Javalin app) {
        app.get("/reports/occupancy", this::getOccupancyReport);
        app.get("/reports/revenue", this::getRevenueReport);
    }

    private void getOccupancyReport(io.javalin.http.Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null || dateStr.isEmpty()) {
                ctx.status(400);
                ctx.json(new java.util.HashMap<String, Object>() {{
                    put("error", "INVALID_REQUEST");
                    put("message", "Date parameter is required");
                }});
                return;
            }

            LocalDate date = LocalDate.parse(dateStr);
            String csv = generateOccupancyReport(date);

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new java.util.HashMap<String, Object>() {{
                put("error", "INTERNAL_ERROR");
                put("message", "Failed to generate report");
            }});
        }
    }

    private void getRevenueReport(io.javalin.http.Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                ctx.status(400);
                ctx.json(new java.util.HashMap<String, Object>() {{
                    put("error", "INVALID_REQUEST");
                    put("message", "Month parameter is required");
                }});
                return;
            }

            YearMonth month = YearMonth.parse(monthStr);
            String csv = generateRevenueReport(month);

            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new java.util.HashMap<String, Object>() {{
                put("error", "INTERNAL_ERROR");
                put("message", "Failed to generate report");
            }});
        }
    }

    private String generateOccupancyReport(LocalDate date) throws SQLException {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        int occupiedCount = 0;
        int totalRooms = 5;

        List<OccupancyRow> rows = new ArrayList<>();

        try (Connection conn = database.getConnection()) {
            var stmt = conn.prepareStatement("""
                SELECT r.room_number, r.id, r.check_in, r.check_out, r.status
                FROM reservations r
                WHERE r.check_in <= ? AND r.check_out > ?
                  AND r.status != ?
                ORDER BY r.room_number
                """);
            stmt.setDate(1, java.sql.Date.valueOf(date));
            stmt.setDate(2, java.sql.Date.valueOf(date));
            stmt.setString(3, ReservationStatus.CANCELLED.value);

            ResultSet rs = stmt.executeQuery();
            Set<Integer> occupiedRooms = new HashSet<>();

            while (rs.next()) {
                int roomNumber = rs.getInt("room_number");
                occupiedRooms.add(roomNumber);
                occupiedCount++;

                String type = getRoomTypeName(roomNumber);
                rows.add(new OccupancyRow(
                    roomNumber,
                    type,
                    rs.getInt("id"),
                    rs.getDate("check_in").toLocalDate(),
                    rs.getDate("check_out").toLocalDate()
                ));
            }
        }

        for (OccupancyRow row : rows) {
            csv.append(row.roomNumber).append(",")
               .append(row.type).append(",")
               .append(row.reservationId).append(",")
               .append(row.checkIn).append(",")
               .append(row.checkOut).append("\n");
        }

        csv.append("total,").append(occupiedCount).append("/").append(totalRooms).append("\n");

        return csv.toString();
    }

    private String generateRevenueReport(YearMonth month) throws SQLException {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        double totalAmount = 0;

        try (Connection conn = database.getConnection()) {
            var stmt = conn.prepareStatement("""
                SELECT r.id, r.room_number, r.check_in, r.check_out, r.status,
                       r.amount_paid, r.refund, r.total
                FROM reservations r
                WHERE EXTRACT(YEAR FROM r.check_in) = ?
                  AND EXTRACT(MONTH FROM r.check_in) = ?
                  AND (r.status = ? OR r.status = ? OR r.status = ? OR r.status = ?)
                ORDER BY r.check_in, r.id
                """);
            stmt.setInt(1, month.getYear());
            stmt.setInt(2, month.getMonthValue());
            stmt.setString(3, ReservationStatus.CONFIRMED.value);
            stmt.setString(4, ReservationStatus.CHECKED_IN.value);
            stmt.setString(5, ReservationStatus.CHECKED_OUT.value);
            stmt.setString(6, ReservationStatus.CANCELLED.value);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                int roomNumber = rs.getInt("room_number");
                LocalDate checkIn = rs.getDate("check_in").toLocalDate();
                LocalDate checkOut = rs.getDate("check_out").toLocalDate();
                String status = rs.getString("status");
                double amountPaid = rs.getDouble("amount_paid");
                Double refund = rs.getObject("refund") != null ? rs.getDouble("refund") : null;

                double netAmount = amountPaid - (refund != null ? refund : 0);
                totalAmount += netAmount;

                csv.append(id).append(",")
                   .append(roomNumber).append(",")
                   .append(checkIn).append(",")
                   .append(checkOut).append(",")
                   .append(status).append(",")
                   .append(String.format("%.2f", netAmount)).append("\n");
            }
        }

        csv.append("total,").append(String.format("%.2f", totalAmount)).append("\n");

        return csv.toString();
    }

    private String getRoomTypeName(int roomNumber) {
        return switch (roomNumber) {
            case 101, 102 -> "STANDARD";
            case 201, 202 -> "DELUXE";
            case 301 -> "SUITE";
            default -> "UNKNOWN";
        };
    }

    static class OccupancyRow {
        int roomNumber;
        String type;
        int reservationId;
        LocalDate checkIn;
        LocalDate checkOut;

        OccupancyRow(int roomNumber, String type, int reservationId, LocalDate checkIn, LocalDate checkOut) {
            this.roomNumber = roomNumber;
            this.type = type;
            this.reservationId = reservationId;
            this.checkIn = checkIn;
            this.checkOut = checkOut;
        }
    }
}
