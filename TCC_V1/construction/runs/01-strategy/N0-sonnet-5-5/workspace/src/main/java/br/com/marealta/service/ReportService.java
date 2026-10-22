package br.com.marealta.service;

import br.com.marealta.db.Database;
import br.com.marealta.db.Repository;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;

public class ReportService {
    private final Database db;

    public ReportService(Database db) { this.db = db; }

    public String occupancy(LocalDate night) {
        return db.inTransaction(c -> {
            StringBuilder sb = new StringBuilder("room,type,reservation,check_in,check_out\n");
            int occupied = 0;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT r.room_number, m.type, r.id, r.check_in, r.check_out FROM reservations r "
                            + "JOIN rooms m ON m.number = r.room_number "
                            + "WHERE r.status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT') "
                            + "AND r.check_in <= ? AND r.check_out > ? ORDER BY r.room_number, r.id")) {
                ps.setDate(1, Date.valueOf(night));
                ps.setDate(2, Date.valueOf(night));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        occupied++;
                        sb.append(rs.getInt(1)).append(',').append(rs.getString(2)).append(',').append(rs.getLong(3))
                                .append(',').append(rs.getDate(4).toLocalDate()).append(',')
                                .append(rs.getDate(5).toLocalDate()).append('\n');
                    }
                }
            }
            int totalRooms = Repository.rooms(c).size();
            return sb.append("total,").append(occupied).append('/').append(totalRooms).append('\n').toString();
        });
    }

    public String revenue(YearMonth month) {
        return db.inTransaction(c -> {
            StringBuilder sb = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
            BigDecimal sum = BigDecimal.ZERO.setScale(2);
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, room_number, check_in, check_out, status, amount_paid - COALESCE(refund, 0) AS amount "
                            + "FROM reservations WHERE check_in >= ? AND check_in < ? "
                            + "AND amount_paid - COALESCE(refund, 0) > 0 ORDER BY check_in, id")) {
                ps.setDate(1, Date.valueOf(month.atDay(1)));
                ps.setDate(2, Date.valueOf(month.plusMonths(1).atDay(1)));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        BigDecimal amount = rs.getBigDecimal(6);
                        sum = sum.add(amount);
                        sb.append(rs.getLong(1)).append(',').append(rs.getInt(2)).append(',')
                                .append(rs.getDate(3).toLocalDate()).append(',').append(rs.getDate(4).toLocalDate())
                                .append(',').append(rs.getString(5)).append(',').append(amount.toPlainString()).append('\n');
                    }
                }
            }
            return sb.append("total,").append(sum.toPlainString()).append('\n').toString();
        });
    }
}
