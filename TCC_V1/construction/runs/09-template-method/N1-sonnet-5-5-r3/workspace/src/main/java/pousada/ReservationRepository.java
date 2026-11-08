package pousada;

import java.sql.Connection;
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

public final class ReservationRepository {
    private static final String SELECT = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan,
                   nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            """;

    public Reservation insert(Connection c, Reservation r) throws SQLException {
        String sql = """
                INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
                  rate_plan, nights, subtotal, discount, total, status, amount_paid, refund)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.roomNumber());
            ps.setString(2, r.guestName());
            ps.setString(3, r.guestEmail());
            ps.setString(4, r.guestCpf());
            ps.setDate(5, Date.valueOf(r.checkIn()));
            ps.setDate(6, Date.valueOf(r.checkOut()));
            ps.setInt(7, r.guests());
            ps.setString(8, r.ratePlan().name());
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
                return r.withId(keys.getLong("id"));
            }
        }
    }

    public void update(Connection c, Reservation r) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
            ps.setString(1, r.status().name());
            ps.setBigDecimal(2, r.amountPaid());
            ps.setBigDecimal(3, r.refund());
            ps.setLong(4, r.id());
            ps.executeUpdate();
        }
    }

    public Optional<Reservation> find(Connection c, long id, boolean lock) throws SQLException {
        List<Reservation> found = query(c, "WHERE id = ?" + (lock ? " FOR UPDATE" : ""), ps -> ps.setLong(1, id));
        return found.stream().findFirst();
    }

    /** Reservas não canceladas do quarto que ocupam alguma das noites de [checkIn, checkOut). */
    public boolean hasOverlap(Connection c, int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT 1 FROM reservations
                WHERE room_number = ? AND status <> 'CANCELLED' AND check_in < ? AND check_out > ?
                """)) {
            ps.setInt(1, roomNumber);
            ps.setDate(2, Date.valueOf(checkOut));
            ps.setDate(3, Date.valueOf(checkIn));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Reservas que começam no mês, por data de entrada e depois por número. */
    public List<Reservation> startingIn(Connection c, YearMonth month) throws SQLException {
        return query(c, "WHERE check_in >= ? AND check_in < ? ORDER BY check_in, id", ps -> {
            ps.setDate(1, Date.valueOf(month.atDay(1)));
            ps.setDate(2, Date.valueOf(month.plusMonths(1).atDay(1)));
        });
    }

    /** Reservas que ocupam um quarto na noite informada, por número do quarto. */
    public List<Reservation> occupying(Connection c, LocalDate night) throws SQLException {
        return query(c, "WHERE check_in <= ? AND check_out > ? ORDER BY room_number, id", ps -> {
            ps.setDate(1, Date.valueOf(night));
            ps.setDate(2, Date.valueOf(night));
        });
    }

    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private List<Reservation> query(Connection c, String tail, Binder binder) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT + tail)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<Reservation> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        }
    }

    private static Reservation map(ResultSet rs) throws SQLException {
        return new Reservation(
                rs.getLong("id"),
                rs.getInt("room_number"),
                rs.getString("guest_name"),
                rs.getString("guest_email"),
                rs.getString("guest_cpf"),
                rs.getDate("check_in").toLocalDate(),
                rs.getDate("check_out").toLocalDate(),
                rs.getInt("guests"),
                RatePlan.valueOf(rs.getString("rate_plan")),
                rs.getInt("nights"),
                rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("discount"),
                rs.getBigDecimal("total"),
                ReservationStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("amount_paid"),
                rs.getBigDecimal("refund"));
    }
}
