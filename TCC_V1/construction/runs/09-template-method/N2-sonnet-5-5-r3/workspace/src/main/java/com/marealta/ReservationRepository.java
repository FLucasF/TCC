package com.marealta;

import javax.sql.DataSource;
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

public final class ReservationRepository {
    private final DataSource dataSource;

    public ReservationRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void insert(Reservation r) {
        String sql = """
                INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                    guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
                r.assignId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Só o que muda depois de criada: situação, valor pago e valor devolvido. */
    public void update(Reservation r) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
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
        return query("SELECT * FROM reservations WHERE id = ?", ps -> ps.setLong(1, id)).stream().findFirst();
    }

    /** Reservas não canceladas do quarto que ocupam alguma noite de [checkIn, checkOut). */
    public boolean existsOverlap(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        return !query("SELECT * FROM reservations WHERE room_number = ? AND status <> 'CANCELLED'"
                + " AND check_in < ? AND check_out > ?", ps -> {
            ps.setInt(1, roomNumber);
            ps.setDate(2, Date.valueOf(checkOut));
            ps.setDate(3, Date.valueOf(checkIn));
        }).isEmpty();
    }

    /** Reservas com entrada em [from, until), em ordem de entrada e depois de número. */
    public List<Reservation> startingBetween(LocalDate from, LocalDate until) {
        return query("SELECT * FROM reservations WHERE check_in >= ? AND check_in < ? ORDER BY check_in, id", ps -> {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(until));
        });
    }

    /** Reservas que ocupam a noite, em ordem de quarto. */
    public List<Reservation> occupying(LocalDate night) {
        return query("SELECT * FROM reservations WHERE check_in <= ? AND check_out > ? ORDER BY room_number, id", ps -> {
            ps.setDate(1, Date.valueOf(night));
            ps.setDate(2, Date.valueOf(night));
        });
    }

    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private List<Reservation> query(String sql, Binder binder) {
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<Reservation> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(read(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Reservation read(ResultSet rs) throws SQLException {
        Quote quote = new Quote(rs.getInt("nights"), Money.round(rs.getBigDecimal("subtotal")),
                Money.round(rs.getBigDecimal("discount")), Money.round(rs.getBigDecimal("total")));
        var refund = rs.getBigDecimal("refund");
        return new Reservation(rs.getLong("id"), rs.getInt("room_number"), rs.getString("guest_name"),
                rs.getString("guest_email"), rs.getString("guest_cpf"),
                rs.getObject("check_in", LocalDate.class), rs.getObject("check_out", LocalDate.class),
                rs.getInt("guests"), RatePlan.valueOf(rs.getString("rate_plan")), quote,
                ReservationStatus.valueOf(rs.getString("status")), Money.round(rs.getBigDecimal("amount_paid")),
                refund == null ? null : Money.round(refund));
    }
}
