package pousada.persistence;

import pousada.domain.Reservation;
import pousada.domain.ReservationStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    private static final String SELECT = "SELECT * FROM reservations ";

    public Reservation insert(Connection c, Reservation r) {
        return Sql.unchecked(() -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                      guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, r.roomNumber());
                ps.setString(2, r.guestName());
                ps.setString(3, r.guestEmail());
                ps.setString(4, r.guestCpf());
                ps.setObject(5, r.checkIn());
                ps.setObject(6, r.checkOut());
                ps.setInt(7, r.guests());
                ps.setString(8, r.ratePlan());
                ps.setInt(9, r.nights());
                ps.setBigDecimal(10, r.subtotal());
                ps.setBigDecimal(11, r.discount());
                ps.setBigDecimal(12, r.total());
                ps.setString(13, r.status().name());
                ps.setBigDecimal(14, r.amountPaid());
                ps.setBigDecimal(15, r.refund());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return find(c, keys.getLong("id")).orElseThrow();
                }
            }
        });
    }

    public void updateState(Connection c, Reservation r) {
        Sql.unchecked(() -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
                ps.setString(1, r.status().name());
                ps.setBigDecimal(2, r.amountPaid());
                ps.setBigDecimal(3, r.refund());
                ps.setLong(4, r.id());
                ps.executeUpdate();
                return null;
            }
        });
    }

    public Optional<Reservation> find(Connection c, long id) {
        return one(c, SELECT + "WHERE id = ?", id);
    }

    public Optional<Reservation> findForUpdate(Connection c, long id) {
        return one(c, SELECT + "WHERE id = ? FOR UPDATE", id);
    }

    /** Há reserva não cancelada do quarto que cruze alguma noite de [checkIn, checkOut)? */
    public boolean hasOverlap(Connection c, int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        return Sql.unchecked(() -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT 1 FROM reservations
                    WHERE room_number = ? AND status <> 'CANCELLED' AND check_in < ? AND check_out > ?""")) {
                ps.setInt(1, roomNumber);
                ps.setObject(2, checkOut);
                ps.setObject(3, checkIn);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    /** Reservas pagas que ocupam o quarto na noite informada, por número de quarto. */
    public List<Reservation> findPaidOnNight(Connection c, LocalDate night) {
        return list(c, SELECT + "WHERE status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT') "
                + "AND check_in <= ? AND check_out > ? ORDER BY room_number, id", night, night);
    }

    public List<Reservation> findStartingIn(Connection c, YearMonth month) {
        return list(c, SELECT + "WHERE check_in >= ? AND check_in < ? ORDER BY check_in, id",
                month.atDay(1), month.plusMonths(1).atDay(1));
    }

    private Optional<Reservation> one(Connection c, String sql, Object param) {
        List<Reservation> found = list(c, sql, param);
        return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
    }

    private List<Reservation> list(Connection c, String sql, Object... params) {
        return Sql.unchecked(() -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Reservation> result = new ArrayList<>();
                    while (rs.next()) result.add(map(rs));
                    return result;
                }
            }
        });
    }

    private static Reservation map(ResultSet rs) throws SQLException {
        return new Reservation(rs.getLong("id"), rs.getInt("room_number"), rs.getString("guest_name"),
                rs.getString("guest_email"), rs.getString("guest_cpf"),
                rs.getObject("check_in", LocalDate.class), rs.getObject("check_out", LocalDate.class),
                rs.getInt("guests"), rs.getString("rate_plan"), rs.getInt("nights"),
                rs.getBigDecimal("subtotal"), rs.getBigDecimal("discount"), rs.getBigDecimal("total"),
                ReservationStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("amount_paid"), rs.getBigDecimal("refund"));
    }
}
