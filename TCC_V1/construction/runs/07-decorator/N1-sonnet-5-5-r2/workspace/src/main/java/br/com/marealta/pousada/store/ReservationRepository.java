package br.com.marealta.pousada.store;

import br.com.marealta.pousada.domain.RatePlan;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.Status;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {

    public boolean existsOverlapping(Connection c, int roomNumber, LocalDate checkIn, LocalDate checkOut)
            throws SQLException {
        String sql = "SELECT 1 FROM reservations WHERE room_number = ? AND status <> 'CANCELLED' "
                + "AND check_in < ? AND check_out > ? LIMIT 1";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, roomNumber);
            s.setDate(2, Date.valueOf(checkOut));
            s.setDate(3, Date.valueOf(checkIn));
            try (ResultSet rs = s.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Reservation insert(Connection c, Reservation r) throws SQLException {
        String sql = "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
                + "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            bind(s, r);
            try (ResultSet rs = s.executeQuery()) {
                rs.next();
                long id = rs.getLong(1);
                return find(c, id).orElseThrow();
            }
        }
    }

    public Optional<Reservation> find(Connection c, long id) throws SQLException {
        return findWhere(c, "SELECT * FROM reservations WHERE id = ?", id);
    }

    public Optional<Reservation> findForUpdate(Connection c, long id) throws SQLException {
        return findWhere(c, "SELECT * FROM reservations WHERE id = ? FOR UPDATE", id);
    }

    public void update(Connection c, Reservation r) throws SQLException {
        String sql = "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, r.status().name());
            s.setBigDecimal(2, r.amountPaid());
            s.setBigDecimal(3, r.refund());
            s.setLong(4, r.id());
            s.executeUpdate();
        }
    }

    /** Reservas pagas (e não canceladas) que ocupam o quarto na noite informada. */
    public List<Reservation> findPaidOccupying(Connection c, LocalDate night) throws SQLException {
        String sql = "SELECT * FROM reservations WHERE status = ANY(?) AND check_in <= ? AND check_out > ? "
                + "ORDER BY room_number, id";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setArray(1, c.createArrayOf("varchar", paidStatuses()));
            s.setDate(2, Date.valueOf(night));
            s.setDate(3, Date.valueOf(night));
            return list(s);
        }
    }

    public List<Reservation> findStartingBetween(Connection c, LocalDate first, LocalDate last) throws SQLException {
        String sql = "SELECT * FROM reservations WHERE check_in >= ? AND check_in <= ? ORDER BY check_in, id";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setDate(1, Date.valueOf(first));
            s.setDate(2, Date.valueOf(last));
            return list(s);
        }
    }

    private Optional<Reservation> findWhere(Connection c, String sql, long id) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, id);
            List<Reservation> found = list(s);
            return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
        }
    }

    private List<Reservation> list(PreparedStatement s) throws SQLException {
        try (ResultSet rs = s.executeQuery()) {
            List<Reservation> result = new ArrayList<>();
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        }
    }

    private String[] paidStatuses() {
        return Arrays.stream(Status.values()).filter(Status::paid).map(Enum::name).toArray(String[]::new);
    }

    private void bind(PreparedStatement s, Reservation r) throws SQLException {
        s.setInt(1, r.roomNumber());
        s.setString(2, r.guestName());
        s.setString(3, r.guestEmail());
        s.setString(4, r.guestCpf());
        s.setDate(5, Date.valueOf(r.checkIn()));
        s.setDate(6, Date.valueOf(r.checkOut()));
        s.setInt(7, r.guests());
        s.setString(8, r.ratePlan().name());
        s.setInt(9, r.nights());
        s.setBigDecimal(10, r.subtotal());
        s.setBigDecimal(11, r.discount());
        s.setBigDecimal(12, r.total());
        s.setString(13, r.status().name());
        s.setBigDecimal(14, r.amountPaid());
        s.setBigDecimal(15, r.refund());
    }

    private Reservation map(ResultSet rs) throws SQLException {
        return new Reservation(
                rs.getLong("id"),
                rs.getInt("room_number"),
                rs.getString("guest_name"),
                rs.getString("guest_email"),
                rs.getString("guest_cpf"),
                rs.getObject("check_in", LocalDate.class),
                rs.getObject("check_out", LocalDate.class),
                rs.getInt("guests"),
                RatePlan.valueOf(rs.getString("rate_plan")),
                rs.getInt("nights"),
                rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("discount"),
                rs.getBigDecimal("total"),
                Status.valueOf(rs.getString("status")),
                rs.getBigDecimal("amount_paid"),
                rs.getBigDecimal("refund"));
    }
}
