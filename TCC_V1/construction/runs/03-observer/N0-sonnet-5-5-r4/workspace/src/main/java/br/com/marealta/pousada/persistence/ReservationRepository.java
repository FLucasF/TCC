package br.com.marealta.pousada.persistence;

import br.com.marealta.pousada.domain.RatePlan;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.ReservationStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ReservationRepository {
    private static final String COLUMNS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
            + "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund";
    private static final String PAID = "status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT')";
    private final Connection c;

    ReservationRepository(Connection c) {
        this.c = c;
    }

    public Reservation insert(Reservation r) {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
                    rate_plan, nights, subtotal, discount, total, status, amount_paid, refund)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id""")) {
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
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return r.withId(rs.getLong(1));
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public void update(Reservation r) {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
            ps.setString(1, r.status().name());
            ps.setBigDecimal(2, r.amountPaid());
            ps.setBigDecimal(3, r.refund());
            ps.setLong(4, r.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public Optional<Reservation> find(long id) {
        return queryOne("SELECT " + COLUMNS + " FROM reservations WHERE id = ?", id);
    }

    /** Busca travando a linha, para que duas operações na mesma reserva não se atropelem. */
    public Optional<Reservation> findForUpdate(long id) {
        return queryOne("SELECT " + COLUMNS + " FROM reservations WHERE id = ? FOR UPDATE", id);
    }

    /** Há reserva não cancelada do quarto em alguma das noites de [checkIn, checkOut)? */
    public boolean hasOverlap(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM reservations WHERE room_number = ? AND status <> 'CANCELLED' "
                        + "AND check_in < ? AND check_out > ? LIMIT 1")) {
            ps.setInt(1, roomNumber);
            ps.setObject(2, checkOut);
            ps.setObject(3, checkIn);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Reservas pagas que ocupam um quarto na noite indicada, por número do quarto. */
    public List<Reservation> findPaidOccupying(LocalDate night) {
        return queryList("SELECT " + COLUMNS + " FROM reservations WHERE " + PAID
                + " AND check_in <= ? AND check_out > ? ORDER BY room_number, id", night, night);
    }

    /** Reservas que começam no período [from, to) e ficaram com dinheiro da pousada. */
    public List<Reservation> findKeepingMoneyStartingBetween(LocalDate from, LocalDate to) {
        return queryList("SELECT " + COLUMNS + " FROM reservations WHERE check_in >= ? AND check_in < ? "
                + "AND amount_paid - COALESCE(refund, 0) > 0 ORDER BY check_in, id", from, to);
    }

    private Optional<Reservation> queryOne(String sql, long id) {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<Reservation> queryList(String sql, LocalDate a, LocalDate b) {
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
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Reservation map(ResultSet rs) throws SQLException {
        return new Reservation(rs.getLong("id"), rs.getInt("room_number"), rs.getString("guest_name"),
                rs.getString("guest_email"), rs.getString("guest_cpf"), rs.getObject("check_in", LocalDate.class),
                rs.getObject("check_out", LocalDate.class), rs.getInt("guests"),
                RatePlan.valueOf(rs.getString("rate_plan")), rs.getInt("nights"), rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("discount"), rs.getBigDecimal("total"),
                ReservationStatus.valueOf(rs.getString("status")), rs.getBigDecimal("amount_paid"),
                rs.getBigDecimal("refund"));
    }
}
