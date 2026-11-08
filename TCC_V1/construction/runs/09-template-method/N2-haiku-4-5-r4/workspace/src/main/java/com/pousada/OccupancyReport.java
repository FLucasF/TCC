package com.pousada;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OccupancyReport extends Report {
    private LocalDate date;

    public OccupancyReport(String dateStr) {
        super("occupancy", dateStr);
        try {
            this.date = DateUtils.parseIsoDate(dateStr);
        } catch (Exception e) {
            throw new ApiException(400, "INVALID_REQUEST", "Data inválida. Use o formato yyyy-MM-dd");
        }
    }

    @Override
    protected String getHeader() {
        return "room,type,reservation,check_in,check_out";
    }

    @Override
    protected List<String> getLines() {
        List<String> lines = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            String query = String.format(
                "SELECT r.id, r.room_number, room.type, r.check_in, r.check_out " +
                "FROM reservations r " +
                "JOIN rooms room ON r.room_number = room.number " +
                "WHERE r.status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') " +
                "AND r.check_in <= '%s' " +
                "AND r.check_out > '%s' " +
                "ORDER BY r.room_number",
                period, period
            );

            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                String line = String.format(
                    "%d,%s,%d,%s,%s",
                    rs.getInt("room_number"),
                    rs.getString("type"),
                    rs.getInt("id"),
                    rs.getString("check_in"),
                    rs.getString("check_out")
                );
                lines.add(line);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error generating occupancy report", e);
        }
        return lines;
    }

    @Override
    protected String getTotal() {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            String query = String.format(
                "SELECT COUNT(DISTINCT r.room_number) as occupied " +
                "FROM reservations r " +
                "WHERE r.status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') " +
                "AND r.check_in <= '%s' " +
                "AND r.check_out > '%s'",
                period, period
            );

            ResultSet rs = stmt.executeQuery(query);
            rs.next();
            int occupied = rs.getInt("occupied");
            return String.format("total,%d/5", occupied);
        } catch (SQLException e) {
            throw new RuntimeException("Error generating total", e);
        }
    }
}
