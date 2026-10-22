package br.com.marealta;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.YearMonth;

public final class ReportService {
    private final Db db;

    public ReportService(Db db) {
        this.db = db;
    }

    public String occupancy(LocalDate night) {
        return db.tx(c -> {
            StringBuilder sb = new StringBuilder("room,type,reservation,check_in,check_out\n");
            int occupied = 0;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT r.room_number, m.type, r.id, r.check_in, r.check_out FROM reservations r "
                            + "JOIN rooms m ON m.number = r.room_number "
                            + "WHERE r.status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT') "
                            + "AND r.check_in <= ? AND r.check_out > ? ORDER BY r.room_number, r.id")) {
                ps.setObject(1, night);
                ps.setObject(2, night);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        occupied++;
                        sb.append(rs.getInt(1)).append(',').append(rs.getString(2)).append(',')
                                .append(rs.getLong(3)).append(',')
                                .append(rs.getObject(4, LocalDate.class)).append(',')
                                .append(rs.getObject(5, LocalDate.class)).append('\n');
                    }
                }
            }
            int total;
            try (PreparedStatement ps = c.prepareStatement("SELECT count(*) FROM rooms");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                total = rs.getInt(1);
            }
            return sb.append("total,").append(occupied).append('/').append(total).append('\n').toString();
        });
    }

    public String revenue(YearMonth month) {
        return db.tx(c -> {
            StringBuilder sb = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
            BigDecimal sum = Money.ZERO;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, room_number, check_in, check_out, status, amount_paid - COALESCE(refund, 0) AS net "
                            + "FROM reservations WHERE check_in >= ? AND check_in < ? "
                            + "AND amount_paid - COALESCE(refund, 0) > 0 ORDER BY check_in, id")) {
                ps.setObject(1, month.atDay(1));
                ps.setObject(2, month.plusMonths(1).atDay(1));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        BigDecimal net = rs.getBigDecimal(6);
                        sum = sum.add(net);
                        sb.append(rs.getLong(1)).append(',').append(rs.getInt(2)).append(',')
                                .append(rs.getObject(3, LocalDate.class)).append(',')
                                .append(rs.getObject(4, LocalDate.class)).append(',')
                                .append(rs.getString(5)).append(',').append(Money.plain(net)).append('\n');
                    }
                }
            }
            return sb.append("total,").append(Money.plain(sum)).append('\n').toString();
        });
    }
}
