package br.com.pousadamarealta.reservas.persistence;

import br.com.pousadamarealta.reservas.domain.RatePlan;
import br.com.pousadamarealta.reservas.domain.Reservation;
import br.com.pousadamarealta.reservas.domain.ReservationStatus;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    private static final String COLUMNS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
            + "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund";

    private final Database db;

    public ReservationRepository(Database db) {
        this.db = db;
    }

    /** Grava uma reserva nova (o id recebido é ignorado) e devolve-a com o id gerado. */
    public Reservation insert(Reservation r) {
        try (PreparedStatement ps = db.connection().prepareStatement(
                "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, "
                        + "rate_plan, nights, subtotal, discount, total, status, amount_paid, refund) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
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
                long id = keys.getLong("id");
                return new Reservation(id, r.roomNumber(), r.guestName(), r.guestEmail(), r.guestCpf(), r.checkIn(),
                        r.checkOut(), r.guests(), r.ratePlan(), r.nights(), r.subtotal(), r.discount(), r.total(),
                        r.status(), r.amountPaid(), r.refund());
            }
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    public Optional<Reservation> find(long id, boolean forUpdate) {
        return queryOne("SELECT " + COLUMNS + " FROM reservations WHERE id = ?" + (forUpdate ? " FOR UPDATE" : ""), id);
    }

    public void update(Reservation r) {
        try (PreparedStatement ps = db.connection().prepareStatement(
                "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
            ps.setString(1, r.status().name());
            ps.setBigDecimal(2, r.amountPaid());
            ps.setBigDecimal(3, r.refund());
            ps.setLong(4, r.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    /** Há reserva não cancelada do quarto que ocupe alguma noite de [checkIn, checkOut)? */
    public boolean existsOverlap(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        try (PreparedStatement ps = db.connection().prepareStatement(
                "SELECT 1 FROM reservations WHERE room_number = ? AND status <> 'CANCELLED' "
                        + "AND check_in < ? AND check_out > ?")) {
            ps.setInt(1, roomNumber);
            ps.setDate(2, Date.valueOf(checkOut));
            ps.setDate(3, Date.valueOf(checkIn));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    /** Reservas pagas que ocupam o quarto na noite indicada, por número do quarto. */
    public List<Reservation> findPaidOccupying(LocalDate night) {
        return queryList("SELECT " + COLUMNS + " FROM reservations "
                        + "WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') AND check_in <= ? AND check_out > ? "
                        + "ORDER BY room_number, id",
                Date.valueOf(night), Date.valueOf(night));
    }

    /** Reservas que começam em [from, to) e deixaram dinheiro com a pousada (pago - devolvido > 0). */
    public List<Reservation> findWithRevenueStartingBetween(LocalDate from, LocalDate to) {
        return queryList("SELECT " + COLUMNS + " FROM reservations "
                        + "WHERE check_in >= ? AND check_in < ? AND amount_paid - COALESCE(refund, 0) > 0 "
                        + "ORDER BY check_in, id",
                Date.valueOf(from), Date.valueOf(to));
    }

    private Optional<Reservation> queryOne(String sql, long id) {
        try (PreparedStatement ps = db.connection().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    private List<Reservation> queryList(String sql, Date a, Date b) {
        try (PreparedStatement ps = db.connection().prepareStatement(sql)) {
            ps.setDate(1, a);
            ps.setDate(2, b);
            try (ResultSet rs = ps.executeQuery()) {
                List<Reservation> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    private static Reservation map(ResultSet rs) throws SQLException {
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
