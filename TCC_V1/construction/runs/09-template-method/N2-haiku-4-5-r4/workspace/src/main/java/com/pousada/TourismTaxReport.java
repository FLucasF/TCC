package com.pousada;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class TourismTaxReport extends Report {
    private YearMonth yearMonth;
    private static final double TAX_PER_GUEST_PER_NIGHT = 4.50;

    public TourismTaxReport(String monthStr) {
        super("tourism-tax", monthStr);
        try {
            this.yearMonth = DateUtils.parseYearMonth(monthStr);
        } catch (Exception e) {
            throw new ApiException(400, "INVALID_REQUEST", "Mês inválido. Use o formato yyyy-MM");
        }
    }

    @Override
    protected String getHeader() {
        return "reservation,room,check_in,check_out,guests,nights,tax";
    }

    @Override
    protected List<String> getLines() {
        List<String> lines = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            String query = String.format(
                "SELECT r.id, r.room_number, r.check_in, r.check_out, r.guests, r.nights " +
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
                int guests = rs.getInt("guests");
                int nights = rs.getInt("nights");
                double tax = Money.round(guests * nights * TAX_PER_GUEST_PER_NIGHT);

                String line = String.format(
                    "%d,%d,%s,%s,%d,%d,%.2f",
                    rs.getInt("id"),
                    rs.getInt("room_number"),
                    rs.getString("check_in"),
                    rs.getString("check_out"),
                    guests,
                    nights,
                    tax
                );
                lines.add(line);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error generating tourism tax report", e);
        }
        return lines;
    }

    @Override
    protected String getTotal() {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            String query = String.format(
                "SELECT SUM(r.guests * r.nights * %.2f) as total " +
                "FROM reservations r " +
                "WHERE r.status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') " +
                "AND EXTRACT(YEAR FROM r.check_in) = %d " +
                "AND EXTRACT(MONTH FROM r.check_in) = %d",
                TAX_PER_GUEST_PER_NIGHT,
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
