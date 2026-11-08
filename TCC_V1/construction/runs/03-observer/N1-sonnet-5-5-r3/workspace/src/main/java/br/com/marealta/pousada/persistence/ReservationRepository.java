package br.com.marealta.pousada.persistence;

import br.com.marealta.pousada.domain.RatePlan;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.ReservationStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {

    private static final String COLUMNS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
            + "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund";

    /** Serializa as reservas de um mesmo quarto até o fim da transação. */
    public void lockRoom(Connection c, int roomNumber) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
            ps.setLong(1, roomNumber);
            ps.execute();
        }
    }

    public boolean isRoomTaken(Connection c, int roomNumber, LocalDate checkIn, LocalDate checkOut)
            throws SQLException {
        String sql = "SELECT 1 FROM reservations WHERE room_number = ? AND status <> 'CANCELLED' "
                + "AND check_in < ? AND check_out > ? LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, roomNumber);
            ps.setDate(2, Date.valueOf(checkOut));
            ps.setDate(3, Date.valueOf(checkIn));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Reservation insert(Connection c, Reservation r) throws SQLException {
        String sql = "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
                + "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            ps.setInt(i++, r.roomNumber());
            ps.setString(i++, r.guestName());
            ps.setString(i++, r.guestEmail());
            ps.setString(i++, r.guestCpf());
            ps.setDate(i++, Date.valueOf(r.checkIn()));
            ps.setDate(i++, Date.valueOf(r.checkOut()));
            ps.setInt(i++, r.guests());
            ps.setString(i++, r.ratePlan().name());
            ps.setInt(i++, r.nights());
            ps.setBigDecimal(i++, r.subtotal());
            ps.setBigDecimal(i++, r.discount());
            ps.setBigDecimal(i++, r.total());
            ps.setString(i++, r.status().name());
            ps.setBigDecimal(i++, r.amountPaid());
            ps.setBigDecimal(i, r.refund());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return withId(r, rs.getLong(1));
            }
        }
    }

    public void update(Connection c, Reservation r) throws SQLException {
        String sql = "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, r.status().name());
            ps.setBigDecimal(2, r.amountPaid());
            ps.setBigDecimal(3, r.refund());
            ps.setLong(4, r.id());
            ps.executeUpdate();
        }
    }

    /** Busca e trava a reserva até o fim da transação. */
    public Optional<Reservation> findForUpdate(Connection c, long id) throws SQLException {
        return queryOne(c, "SELECT " + COLUMNS + " FROM reservations WHERE id = ? FOR UPDATE", id);
    }

    public Optional<Reservation> find(Connection c, long id) throws SQLException {
        return queryOne(c, "SELECT " + COLUMNS + " FROM reservations WHERE id = ?", id);
    }

    /** Reservas pagas (não canceladas e já confirmadas) que ocupam o quarto na noite informada. */
    public List<Reservation> findPaidOccupying(Connection c, LocalDate night) throws SQLException {
        return query(c, "SELECT " + COLUMNS + " FROM reservations WHERE status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT') "
                + "AND check_in <= ? AND check_out > ? ORDER BY room_number", Date.valueOf(night), Date.valueOf(night));
    }

    /** Reservas que começam entre as duas datas (início inclusive, fim exclusive). */
    public List<Reservation> findStartingBetween(Connection c, LocalDate from, LocalDate to) throws SQLException {
        return query(c, "SELECT " + COLUMNS + " FROM reservations WHERE check_in >= ? AND check_in < ? "
                + "ORDER BY check_in, id", Date.valueOf(from), Date.valueOf(to));
    }

    private Optional<Reservation> queryOne(Connection c, String sql, long id) throws SQLException {
        return query(c, sql, id).stream().findFirst();
    }

    private List<Reservation> query(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Reservation> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(map(rs));
                }
                return result;
            }
        }
    }

    private Reservation map(ResultSet rs) throws SQLException {
        BigDecimal refund = rs.getBigDecimal("refund");
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
                refund);
    }

    private Reservation withId(Reservation r, long id) {
        return new Reservation(id, r.roomNumber(), r.guestName(), r.guestEmail(), r.guestCpf(), r.checkIn(),
                r.checkOut(), r.guests(), r.ratePlan(), r.nights(), r.subtotal(), r.discount(), r.total(),
                r.status(), r.amountPaid(), r.refund());
    }
}
