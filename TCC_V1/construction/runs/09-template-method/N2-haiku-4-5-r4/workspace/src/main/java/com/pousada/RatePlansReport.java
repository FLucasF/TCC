package com.pousada;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class RatePlansReport extends Report {
    private YearMonth yearMonth;

    public RatePlansReport(String monthStr) {
        super("rate-plans", monthStr);
        try {
            this.yearMonth = DateUtils.parseYearMonth(monthStr);
        } catch (Exception e) {
            throw new ApiException(400, "INVALID_REQUEST", "Mês inválido. Use o formato yyyy-MM");
        }
    }

    @Override
    protected String getHeader() {
        return "rate_plan,reservations,amount";
    }

    @Override
    protected List<String> getLines() {
        List<String> lines = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            for (RatePlan plan : RatePlan.values()) {
                String query = String.format(
                    "SELECT COUNT(*) as count, SUM(r.amount_paid - COALESCE(r.refund, 0)) as total " +
                    "FROM reservations r " +
                    "WHERE r.status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') " +
                    "AND r.rate_plan = '%s' " +
                    "AND EXTRACT(YEAR FROM r.check_in) = %d " +
                    "AND EXTRACT(MONTH FROM r.check_in) = %d",
                    plan.toString(),
                    yearMonth.getYear(),
                    yearMonth.getMonthValue()
                );

                ResultSet rs = stmt.executeQuery(query);
                rs.next();
                int count = rs.getInt("count");
                double amount = rs.getDouble("total");

                String line = String.format(
                    "%s,%d,%.2f",
                    plan.toString(),
                    count,
                    amount
                );
                lines.add(line);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error generating rate plans report", e);
        }
        return lines;
    }

    @Override
    protected String getTotal() {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            String query = String.format(
                "SELECT COUNT(*) as count, SUM(r.amount_paid - COALESCE(r.refund, 0)) as total " +
                "FROM reservations r " +
                "WHERE r.status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') " +
                "AND EXTRACT(YEAR FROM r.check_in) = %d " +
                "AND EXTRACT(MONTH FROM r.check_in) = %d",
                yearMonth.getYear(),
                yearMonth.getMonthValue()
            );

            ResultSet rs = stmt.executeQuery(query);
            rs.next();
            int count = rs.getInt("count");
            double total = rs.getDouble("total");

            return String.format("total,%d,%.2f", count, total);
        } catch (SQLException e) {
            throw new RuntimeException("Error generating total", e);
        }
    }
}
