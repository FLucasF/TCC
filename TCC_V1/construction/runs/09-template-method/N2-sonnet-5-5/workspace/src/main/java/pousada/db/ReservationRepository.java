package pousada.db;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import pousada.domain.RatePlan;
import pousada.domain.Reservation;
import pousada.domain.ReservationStatus;

public class ReservationRepository {
    private static final String COLUMNS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
            + "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund";

    private final Database db;

    public ReservationRepository(Database db) {
        this.db = db;
    }

    public void insert(Reservation r) {
        db.query(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                        guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, r.getRoomNumber());
                ps.setString(2, r.getGuestName());
                ps.setString(3, r.getGuestEmail());
                ps.setString(4, r.getGuestCpf());
                ps.setDate(5, Date.valueOf(r.getCheckIn()));
                ps.setDate(6, Date.valueOf(r.getCheckOut()));
                ps.setInt(7, r.getGuests());
                ps.setString(8, r.getRatePlan().name());
                ps.setInt(9, r.getNights());
                ps.setBigDecimal(10, r.getSubtotal());
                ps.setBigDecimal(11, r.getDiscount());
                ps.setBigDecimal(12, r.getTotal());
                ps.setString(13, r.getStatus().name());
                ps.setBigDecimal(14, r.getAmountPaid());
                ps.setBigDecimal(15, r.getRefund());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    r.assignId(keys.getLong(1));
                }
            }
            return null;
        });
    }

    public void update(Reservation r) {
        db.query(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
                ps.setString(1, r.getStatus().name());
                ps.setBigDecimal(2, r.getAmountPaid());
                ps.setBigDecimal(3, r.getRefund());
                ps.setLong(4, r.getId());
                ps.executeUpdate();
            }
            return null;
        });
    }

    public Optional<Reservation> find(long id) {
        return db.query(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM reservations WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.<Reservation>empty();
                }
            }
        });
    }

    public List<Reservation> findAll() {
        return db.query(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM reservations ORDER BY id");
                 ResultSet rs = ps.executeQuery()) {
                List<Reservation> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        });
    }

    /** Há reserva não cancelada do quarto que cobre alguma das noites de [checkIn, checkOut)? */
    public boolean hasActiveOverlap(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        return db.query(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT COUNT(*) FROM reservations
                    WHERE room_number = ? AND status <> 'CANCELLED' AND check_in < ? AND check_out > ?""")) {
                ps.setInt(1, roomNumber);
                ps.setDate(2, Date.valueOf(checkOut));
                ps.setDate(3, Date.valueOf(checkIn));
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1) > 0;
                }
            }
        });
    }

    private static Reservation map(ResultSet rs) throws SQLException {
        BigDecimal refund = rs.getBigDecimal("refund");
        return new Reservation(rs.getLong("id"), rs.getInt("room_number"), rs.getString("guest_name"),
                rs.getString("guest_email"), rs.getString("guest_cpf"),
                rs.getDate("check_in").toLocalDate(), rs.getDate("check_out").toLocalDate(),
                rs.getInt("guests"), RatePlan.valueOf(rs.getString("rate_plan")), rs.getInt("nights"),
                rs.getBigDecimal("subtotal"), rs.getBigDecimal("discount"), rs.getBigDecimal("total"),
                ReservationStatus.valueOf(rs.getString("status")), rs.getBigDecimal("amount_paid"), refund);
    }
}
