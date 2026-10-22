package br.com.marealta.persistence;

import br.com.marealta.domain.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.sql.DataSource;

public class ReservationRepository {
    private static final String COLUMNS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
            + "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund";

    private final DataSource ds;

    public ReservationRepository(DataSource ds) { this.ds = ds; }

    /** Grava a reserva se o quarto estiver livre em todas as noites; a trava impede duas reservas simultâneas. */
    public Reservation insert(Reservation draft) {
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement lock = c.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
                    lock.setLong(1, draft.roomNumber());
                    lock.execute();
                }
                try (PreparedStatement ps = c.prepareStatement("""
                        SELECT 1 FROM reservations
                        WHERE room_number = ? AND status <> 'CANCELLED' AND check_in < ? AND check_out > ?""")) {
                    ps.setInt(1, draft.roomNumber());
                    ps.setDate(2, Date.valueOf(draft.checkOutDate()));
                    ps.setDate(3, Date.valueOf(draft.checkInDate()));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) throw ApiException.roomUnavailable();
                    }
                }
                long id;
                try (PreparedStatement ps = c.prepareStatement("""
                        INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                            guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund)
                        VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id""")) {
                    bind(ps, draft);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        id = rs.getLong(1);
                    }
                }
                c.commit();
                return find(id).orElseThrow();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public Optional<Reservation> find(long id) {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM reservations WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Aplica uma mudança à reserva travada, numa transação, e devolve o resultado. */
    public Reservation modify(long id, Consumer<Reservation> change) {
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                Reservation r;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT " + COLUMNS + " FROM reservations WHERE id = ? FOR UPDATE")) {
                    ps.setLong(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw ApiException.reservationNotFound(id);
                        r = map(rs);
                    }
                }
                change.accept(r);
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
                    ps.setString(1, r.status().name());
                    ps.setBigDecimal(2, r.amountPaid());
                    ps.setBigDecimal(3, r.refund());
                    ps.setLong(4, id);
                    ps.executeUpdate();
                }
                c.commit();
                return r;
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Reservas pagas que ocupam um quarto na noite dada. */
    public List<Reservation> paidOnNight(LocalDate night) {
        return query("WHERE status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT') AND check_in <= ? AND check_out > ? "
                + "ORDER BY room_number, id", Date.valueOf(night), Date.valueOf(night));
    }

    /** Reservas cuja entrada cai no intervalo [from, to). */
    public List<Reservation> startingBetween(LocalDate from, LocalDate to) {
        return query("WHERE check_in >= ? AND check_in < ? ORDER BY check_in, id", Date.valueOf(from), Date.valueOf(to));
    }

    private List<Reservation> query(String clause, Date... params) {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM reservations " + clause)) {
            for (int i = 0; i < params.length; i++) ps.setDate(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                List<Reservation> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void bind(PreparedStatement ps, Reservation r) throws SQLException {
        ps.setInt(1, r.roomNumber());
        ps.setString(2, r.guestName());
        ps.setString(3, r.guestEmail());
        ps.setString(4, r.guestCpf());
        ps.setDate(5, Date.valueOf(r.checkInDate()));
        ps.setDate(6, Date.valueOf(r.checkOutDate()));
        ps.setInt(7, r.guests());
        ps.setString(8, r.ratePlan().name());
        ps.setInt(9, r.nights());
        ps.setBigDecimal(10, r.subtotal());
        ps.setBigDecimal(11, r.discount());
        ps.setBigDecimal(12, r.total());
        ps.setString(13, r.status().name());
        ps.setBigDecimal(14, r.amountPaid());
        ps.setBigDecimal(15, r.refund());
    }

    private static Reservation map(ResultSet rs) throws SQLException {
        Pricing.Quote quote = new Pricing.Quote(rs.getInt("nights"), rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("discount"), rs.getBigDecimal("total"));
        return new Reservation(rs.getLong("id"), rs.getInt("room_number"), rs.getString("guest_name"),
                rs.getString("guest_email"), rs.getString("guest_cpf"),
                rs.getDate("check_in").toLocalDate(), rs.getDate("check_out").toLocalDate(),
                rs.getInt("guests"), RatePlan.valueOf(rs.getString("rate_plan")), quote,
                ReservationStatus.valueOf(rs.getString("status")), rs.getBigDecimal("amount_paid"),
                rs.getBigDecimal("refund"));
    }
}
