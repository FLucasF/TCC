package com.pousada.services;

import com.pousada.database.Database;
import com.pousada.utils.FormatUtils;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class ReportService {
    private final Database db;
    private static final String LODGING_NAME = "Pousada Maré Alta";
    private static final BigDecimal TOURISM_TAX_PER_GUEST_NIGHT = BigDecimal.valueOf(4.50);

    public ReportService(Database db) {
        this.db = db;
    }

    public String getOccupancyReport(LocalDate date) {
        StringBuilder csv = new StringBuilder();
        csv.append(LODGING_NAME).append(",occupancy,").append(FormatUtils.formatDateIso(date)).append("\n");
        csv.append("room,type,reservation,check_in,check_out\n");

        int occupiedRooms = 0;

        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement()) {

            String sql = "SELECT DISTINCT r.number, r.type, res.id, res.check_in, res.check_out " +
                    "FROM rooms r " +
                    "LEFT JOIN reservations res ON r.number = res.room_number " +
                    "AND res.status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') " +
                    "AND res.check_in <= '" + date + "' AND res.check_out > '" + date + "' " +
                    "ORDER BY r.number, res.id";

            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                int roomNumber = rs.getInt("number");
                String roomType = rs.getString("type");
                Object resIdObj = rs.getObject("id");
                java.sql.Date checkIn = rs.getDate("check_in");

                if (resIdObj != null) {
                    long reservationId = rs.getLong("id");
                    java.sql.Date checkOut = rs.getDate("check_out");

                    csv.append(roomNumber).append(",")
                            .append(roomType).append(",")
                            .append(reservationId).append(",")
                            .append(FormatUtils.formatDateIso(checkIn.toLocalDate())).append(",")
                            .append(FormatUtils.formatDateIso(checkOut.toLocalDate())).append("\n");
                    occupiedRooms++;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar relatório de ocupação", e);
        }

        csv.append("total,").append(occupiedRooms).append("/5\n");
        return csv.toString();
    }

    public String getRevenueReport(YearMonth month) {
        StringBuilder csv = new StringBuilder();
        csv.append(LODGING_NAME).append(",revenue,").append(month).append("\n");
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal totalRevenue = BigDecimal.ZERO;

        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement()) {

            String sql = "SELECT id, room_number, check_in, check_out, status, amount_paid, refund " +
                    "FROM reservations " +
                    "WHERE EXTRACT(YEAR FROM check_in) = " + month.getYear() +
                    " AND EXTRACT(MONTH FROM check_in) = " + month.getMonthValue() +
                    " AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED') " +
                    "ORDER BY check_in, id";

            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                long id = rs.getLong("id");
                int roomNumber = rs.getInt("room_number");
                LocalDate checkIn = rs.getDate("check_in").toLocalDate();
                LocalDate checkOut = rs.getDate("check_out").toLocalDate();
                String status = rs.getString("status");
                BigDecimal amountPaid = rs.getBigDecimal("amount_paid");
                BigDecimal refund = rs.getBigDecimal("refund");

                BigDecimal revenue = amountPaid.subtract(refund != null ? refund : BigDecimal.ZERO);

                csv.append(id).append(",")
                        .append(roomNumber).append(",")
                        .append(FormatUtils.formatDateIso(checkIn)).append(",")
                        .append(FormatUtils.formatDateIso(checkOut)).append(",")
                        .append(status).append(",")
                        .append(FormatUtils.formatCurrencyCSV(revenue)).append("\n");

                totalRevenue = totalRevenue.add(revenue);
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar relatório financeiro", e);
        }

        csv.append("total,,,,").append(FormatUtils.formatCurrencyCSV(totalRevenue)).append("\n");
        return csv.toString();
    }

    public String getTouristTaxReport(YearMonth month) {
        StringBuilder csv = new StringBuilder();
        csv.append(LODGING_NAME).append(",tourism-tax,").append(month).append("\n");
        csv.append("reservation,room,check_in,check_out,guests,nights,tax\n");

        BigDecimal totalTax = BigDecimal.ZERO;

        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement()) {

            String sql = "SELECT id, room_number, check_in, check_out, guests, nights " +
                    "FROM reservations " +
                    "WHERE EXTRACT(YEAR FROM check_in) = " + month.getYear() +
                    " AND EXTRACT(MONTH FROM check_in) = " + month.getMonthValue() +
                    " AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') " +
                    "ORDER BY check_in, id";

            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                long id = rs.getLong("id");
                int roomNumber = rs.getInt("room_number");
                LocalDate checkIn = rs.getDate("check_in").toLocalDate();
                LocalDate checkOut = rs.getDate("check_out").toLocalDate();
                int guests = rs.getInt("guests");
                int nights = rs.getInt("nights");

                BigDecimal tax = TOURISM_TAX_PER_GUEST_NIGHT.multiply(BigDecimal.valueOf(guests * nights));

                csv.append(id).append(",")
                        .append(roomNumber).append(",")
                        .append(FormatUtils.formatDateIso(checkIn)).append(",")
                        .append(FormatUtils.formatDateIso(checkOut)).append(",")
                        .append(guests).append(",")
                        .append(nights).append(",")
                        .append(FormatUtils.formatCurrencyCSV(tax)).append("\n");

                totalTax = totalTax.add(tax);
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar relatório de taxa de turismo", e);
        }

        csv.append("total,,,,,,").append(FormatUtils.formatCurrencyCSV(totalTax)).append("\n");
        return csv.toString();
    }

    public String getRatePlansReport(YearMonth month) {
        StringBuilder csv = new StringBuilder();
        csv.append(LODGING_NAME).append(",rate-plans,").append(month).append("\n");
        csv.append("rate_plan,reservations,amount\n");

        BigDecimal standardRevenue = BigDecimal.ZERO;
        BigDecimal nonRefundableRevenue = BigDecimal.ZERO;
        int standardCount = 0;
        int nonRefundableCount = 0;

        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement()) {

            String sql = "SELECT rate_plan, COUNT(*) as count, COALESCE(SUM(amount_paid - COALESCE(refund, 0)), 0) as revenue " +
                    "FROM reservations " +
                    "WHERE EXTRACT(YEAR FROM check_in) = " + month.getYear() +
                    " AND EXTRACT(MONTH FROM check_in) = " + month.getMonthValue() +
                    " AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED') " +
                    "GROUP BY rate_plan";

            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                String ratePlan = rs.getString("rate_plan");
                int count = rs.getInt("count");
                BigDecimal revenue = rs.getBigDecimal("revenue");

                if ("STANDARD".equals(ratePlan)) {
                    standardCount = count;
                    standardRevenue = revenue;
                } else if ("NON_REFUNDABLE".equals(ratePlan)) {
                    nonRefundableCount = count;
                    nonRefundableRevenue = revenue;
                }
            }

            csv.append("STANDARD,").append(standardCount).append(",")
                    .append(FormatUtils.formatCurrencyCSV(standardRevenue)).append("\n");
            csv.append("NON_REFUNDABLE,").append(nonRefundableCount).append(",")
                    .append(FormatUtils.formatCurrencyCSV(nonRefundableRevenue)).append("\n");

            BigDecimal totalRevenue = standardRevenue.add(nonRefundableRevenue);
            int totalCount = standardCount + nonRefundableCount;
            csv.append("total,").append(totalCount).append(",")
                    .append(FormatUtils.formatCurrencyCSV(totalRevenue)).append("\n");

        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar relatório de planos de tarifa", e);
        }

        return csv.toString();
    }
}
