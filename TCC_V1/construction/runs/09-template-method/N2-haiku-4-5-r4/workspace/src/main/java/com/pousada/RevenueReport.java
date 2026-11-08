package com.pousada;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class RevenueReport extends Report {
    private YearMonth yearMonth;

    public RevenueReport(String monthStr) {
        super("revenue", monthStr);
        try {
            this.yearMonth = DateUtils.parseYearMonth(monthStr);
        } catch (Exception e) {
            throw new ApiException(400, "INVALID_REQUEST", "Mês inválido. Use o formato yyyy-MM");
        }
    }

    @Override
    protected String getHeader() {
        return "reservation,room,check_in,check_out,status,amount";
    }

    @Override
    protected List<String> getLines() {
        List<String> lines = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            String query = String.format(
                "SELECT r.id, r.room_number, r.check_in, r.check_out, r.status, " +
                "(r.amount_paid - COALESCE(r.refund, 0)) as amount " +
                "FROM reservations r " +
                "WHERE r.status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') " +
                "AND EXTRACT(YEAR FROM r.check_in) = %d " +
                "AND EXTRACT(MONTH FROM r.check_in) = %d " +
                "ORDER BY r.check_in, r.id",
                yearMonth.getYear(),
                yearMonth.getMonthValue()
            );

            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                double amount = rs.getDouble("amount");
                String line = String.format(
                    "%d,%d,%s,%s,%s,%.2f",
                    rs.getInt("id"),
                    rs.getInt("room_number"),
                    rs.getString("check_in"),
                    rs.getString("check_out"),
                    rs.getString("status"),
                    amount
                );
                lines.add(line);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error generating revenue report", e);
        }
        return lines;
    }

    @Override
    protected String getTotal() {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            String query = String.format(
                "SELECT SUM(r.amount_paid - COALESCE(r.refund, 0)) as total " +
                "FROM reservations r " +
                "WHERE r.status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') " +
                "AND EXTRACT(YEAR FROM r.check_in) = %d " +
                "AND EXTRACT(MONTH FROM r.check_in) = %d",
                yearMonth.getYear(),
                yearMonth.getMonthValue()
            );

            ResultSet rs = stmt.executeQuery(query);
            rs.next();
            double total = rs.getDouble("total");
            return String.format("total,%.2f", total);
        } catch (SQLException e) {
            throw new RuntimeException("Error generating total", e);
        }
    }
}
