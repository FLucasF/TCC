package br.com.mealta.store;

import br.com.mealta.domain.RatePlan;
import br.com.mealta.domain.Reservation;
import br.com.mealta.domain.ReservationStatus;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PgReservationRepository implements ReservationRepository {
    private static final String COLUMNS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, "
            + "check_out, guests, rate_plan, subtotal, discount, total, status, amount_paid, refund";
    private static final String PAID = "status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')";

    private final Database db;

    public PgReservationRepository(Database db) {
        this.db = db;
    }

    @Override
    public Reservation save(Reservation r) {
        return db.withConnection(c -> {
            if (r.id() == null) {
                try (PreparedStatement ps = c.prepareStatement("""
                        INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in,
                            check_out, guests, rate_plan, subtotal, discount, total, status, amount_paid, refund)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, r.roomNumber());
                    ps.setString(2, r.guestName());
                    ps.setString(3, r.guestEmail());
                    ps.setString(4, r.guestCpf());
                    ps.setDate(5, Date.valueOf(r.checkInDate()));
                    ps.setDate(6, Date.valueOf(r.checkOutDate()));
                    ps.setInt(7, r.guests());
                    ps.setString(8, r.ratePlan().name());
                    ps.setBigDecimal(9, r.subtotal());
                    ps.setBigDecimal(10, r.discount());
                    ps.setBigDecimal(11, r.total());
                    ps.setString(12, r.status().name());
                    ps.setBigDecimal(13, r.amountPaid());
                    ps.setBigDecimal(14, r.refund());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        r.assignId(keys.getLong("id"));
                    }
                }
            } else {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
                    ps.setString(1, r.status().name());
                    ps.setBigDecimal(2, r.amountPaid());
                    ps.setBigDecimal(3, r.refund());
                    ps.setLong(4, r.id());
                    ps.executeUpdate();
                }
            }
            return r;
        });
    }

    @Override
    public Optional<Reservation> findById(long id) {
        return queryOne("SELECT " + COLUMNS + " FROM reservations WHERE id = ?", id);
    }

    @Override
    public Optional<Reservation> lockById(long id) {
        return queryOne("SELECT " + COLUMNS + " FROM reservations WHERE id = ? FOR UPDATE", id);
    }

    @Override
    public void lockRoom(int roomNumber) {
        db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
                ps.setLong(1, roomNumber);
                ps.execute();
            }
            return null;
        });
    }

    @Override
    public boolean hasActiveOverlap(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT 1 FROM reservations
                    WHERE room_number = ? AND status <> 'CANCELLED' AND check_in < ? AND check_out > ?
                    LIMIT 1""")) {
                ps.setInt(1, roomNumber);
                ps.setDate(2, Date.valueOf(checkOut));
                ps.setDate(3, Date.valueOf(checkIn));
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    @Override
    public List<Reservation> startingIn(YearMonth month) {
        return queryList("SELECT " + COLUMNS + " FROM reservations WHERE check_in >= ? AND check_in < ? "
                        + "ORDER BY check_in, id",
                Date.valueOf(month.atDay(1)), Date.valueOf(month.plusMonths(1).atDay(1)));
    }

    @Override
    public List<Reservation> paidOccupying(LocalDate night) {
        return queryList("SELECT " + COLUMNS + " FROM reservations WHERE " + PAID
                        + " AND check_in <= ? AND check_out > ? ORDER BY room_number, id",
                Date.valueOf(night), Date.valueOf(night));
    }

    private Optional<Reservation> queryOne(String sql, Object... params) {
        return queryList(sql, params).stream().findFirst();
    }

    private List<Reservation> queryList(String sql, Object... params) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    List<Reservation> found = new ArrayList<>();
                    while (rs.next()) {
                        found.add(read(rs));
                    }
                    return found;
                }
            }
        });
    }

    private static Reservation read(ResultSet rs) throws SQLException {
        return new Reservation(rs.getLong("id"), rs.getInt("room_number"), rs.getString("guest_name"),
                rs.getString("guest_email"), rs.getString("guest_cpf"),
                rs.getDate("check_in").toLocalDate(), rs.getDate("check_out").toLocalDate(),
                rs.getInt("guests"), RatePlan.valueOf(rs.getString("rate_plan")),
                rs.getBigDecimal("subtotal"), rs.getBigDecimal("discount"), rs.getBigDecimal("total"),
                ReservationStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("amount_paid"), rs.getBigDecimal("refund"));
    }
}
