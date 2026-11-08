package br.com.pousadamarealta.reservas.persistence;

import br.com.pousadamarealta.reservas.domain.RatePlan;
import br.com.pousadamarealta.reservas.domain.Reservation;
import br.com.pousadamarealta.reservas.domain.ReservationStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    private static final String COLUMNS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, "
            + "check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund";

    /** Grava uma reserva nova e devolve-a com o id gerado. */
    public Reservation insert(Connection c, Reservation r) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
                        + "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.roomNumber());
            ps.setString(2, r.guestName());
            ps.setString(3, r.guestEmail());
            ps.setString(4, r.guestCpf());
            ps.setObject(5, r.checkIn());
            ps.setObject(6, r.checkOut());
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
                long id = keys.getLong("id");
                return new Reservation(id, r.roomNumber(), r.guestName(), r.guestEmail(), r.guestCpf(),
                        r.checkIn(), r.checkOut(), r.guests(), r.ratePlan(), r.nights(), r.subtotal(),
                        r.discount(), r.total(), r.status(), r.amountPaid(), r.refund());
            }
        }
    }

    public void updateState(Connection c, Reservation r) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
            ps.setString(1, r.status().name());
            ps.setBigDecimal(2, r.amountPaid());
            ps.setBigDecimal(3, r.refund());
            ps.setLong(4, r.id());
            ps.executeUpdate();
        }
    }

    public Optional<Reservation> find(Connection c, long id) throws SQLException {
        return findById(c, id, "");
    }

    /** Busca travando a linha, para que mudanças de situação concorrentes sejam serializadas. */
    public Optional<Reservation> findForUpdate(Connection c, long id) throws SQLException {
        return findById(c, id, " FOR UPDATE");
    }

    private Optional<Reservation> findById(Connection c, long id, String suffix) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM reservations WHERE id = ?" + suffix)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    /** Há reserva não cancelada do quarto que ocupe alguma noite de [checkIn, checkOut)? */
    public boolean hasOverlap(Connection c, int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM reservations WHERE room_number = ? AND status <> 'CANCELLED' "
                        + "AND check_in < ? AND check_out > ? LIMIT 1")) {
            ps.setInt(1, roomNumber);
            ps.setObject(2, checkOut);
            ps.setObject(3, checkIn);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Reservas pagas (confirmadas, com check-in ou check-out) que ocupam o quarto na noite, por quarto. */
    public List<Reservation> findPaidOnNight(Connection c, LocalDate night) throws SQLException {
        return query(c, "SELECT " + COLUMNS + " FROM reservations "
                + "WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') AND check_in <= ? AND check_out > ? "
                + "ORDER BY room_number, id", night, night);
    }

    /** Reservas que começam no intervalo [from, to) e deixaram dinheiro com a pousada. */
    public List<Reservation> findRetainingMoney(Connection c, LocalDate from, LocalDate to) throws SQLException {
        return query(c, "SELECT " + COLUMNS + " FROM reservations "
                + "WHERE check_in >= ? AND check_in < ? AND amount_paid - COALESCE(refund, 0) > 0 "
                + "ORDER BY check_in, id", from, to);
    }

    private List<Reservation> query(Connection c, String sql, LocalDate a, LocalDate b) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, a);
            ps.setObject(2, b);
            try (ResultSet rs = ps.executeQuery()) {
                List<Reservation> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        }
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
                ReservationStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("amount_paid"),
                rs.getBigDecimal("refund"));
    }
}
