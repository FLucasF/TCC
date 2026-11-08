package br.com.mare.pousada.infra;

import br.com.mare.pousada.domain.RatePlan;
import br.com.mare.pousada.domain.Reservation;
import br.com.mare.pousada.domain.ReservationStatus;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {

    /** Reservas de um quarto são serializadas por esta trava, até o fim da transação. */
    public void lockRoom(Connection c, int roomNumber) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
            s.setLong(1, roomNumber);
            s.execute();
        }
    }

    public boolean hasActiveReservation(Connection c, int roomNumber, LocalDate checkIn, LocalDate checkOut)
            throws SQLException {
        try (PreparedStatement s = c.prepareStatement("""
                SELECT 1 FROM reservations
                WHERE room_number = ? AND status <> 'CANCELLED' AND check_in < ? AND check_out > ?
                LIMIT 1""")) {
            s.setInt(1, roomNumber);
            s.setDate(2, Date.valueOf(checkOut));
            s.setDate(3, Date.valueOf(checkIn));
            try (ResultSet rs = s.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Reservation insert(Connection c, Reservation r) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("""
                INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                    guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING *""")) {
            s.setInt(1, r.getRoomNumber());
            s.setString(2, r.getGuestName());
            s.setString(3, r.getGuestEmail());
            s.setString(4, r.getGuestCpf());
            s.setDate(5, Date.valueOf(r.getCheckIn()));
            s.setDate(6, Date.valueOf(r.getCheckOut()));
            s.setInt(7, r.getGuests());
            s.setString(8, r.getRatePlan().name());
            s.setInt(9, r.getNights());
            s.setBigDecimal(10, r.getSubtotal());
            s.setBigDecimal(11, r.getDiscount());
            s.setBigDecimal(12, r.getTotal());
            s.setString(13, r.getStatus().name());
            s.setBigDecimal(14, r.getAmountPaid());
            s.setBigDecimal(15, r.getRefund());
            try (ResultSet rs = s.executeQuery()) {
                rs.next();
                return map(rs);
            }
        }
    }

    public Optional<Reservation> findForUpdate(Connection c, long id) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT * FROM reservations WHERE id = ? FOR UPDATE")) {
            s.setLong(1, id);
            try (ResultSet rs = s.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public void saveState(Connection c, Reservation r) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(
                "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
            s.setString(1, r.getStatus().name());
            s.setBigDecimal(2, r.getAmountPaid());
            s.setBigDecimal(3, r.getRefund());
            s.setLong(4, r.getId());
            s.executeUpdate();
        }
    }

    /** Reservas cuja hospedagem ocupa a noite informada, por número de quarto. */
    public List<Reservation> findStayingOn(Connection c, LocalDate night) throws SQLException {
        return query(c, "SELECT * FROM reservations WHERE check_in <= ? AND check_out > ? ORDER BY room_number, id",
                Date.valueOf(night), Date.valueOf(night));
    }

    /** Reservas que começam no mês, por data de entrada e número. */
    public List<Reservation> findStartingIn(Connection c, YearMonth month) throws SQLException {
        return query(c, "SELECT * FROM reservations WHERE check_in >= ? AND check_in < ? ORDER BY check_in, id",
                Date.valueOf(month.atDay(1)), Date.valueOf(month.plusMonths(1).atDay(1)));
    }

    private List<Reservation> query(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                s.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = s.executeQuery()) {
                List<Reservation> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(map(rs));
                }
                return result;
            }
        }
    }

    public Optional<Reservation> find(Connection c, long id) throws SQLException {
        List<Reservation> found = query(c, "SELECT * FROM reservations WHERE id = ?", id);
        return found.stream().findFirst();
    }

    private Reservation map(ResultSet rs) throws SQLException {
        return new Reservation(rs.getLong("id"), rs.getInt("room_number"), rs.getString("guest_name"),
                rs.getString("guest_email"), rs.getString("guest_cpf"),
                rs.getDate("check_in").toLocalDate(), rs.getDate("check_out").toLocalDate(),
                rs.getInt("guests"), RatePlan.valueOf(rs.getString("rate_plan")), rs.getInt("nights"),
                rs.getBigDecimal("subtotal"), rs.getBigDecimal("discount"), rs.getBigDecimal("total"),
                ReservationStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("amount_paid"), rs.getBigDecimal("refund"));
    }
}
