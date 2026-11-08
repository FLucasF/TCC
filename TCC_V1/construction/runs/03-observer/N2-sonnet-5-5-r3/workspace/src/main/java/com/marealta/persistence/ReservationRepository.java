package com.marealta.persistence;

import com.marealta.domain.Quote;
import com.marealta.domain.RatePlan;
import com.marealta.domain.Reservation;
import com.marealta.domain.ReservationStatus;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Todos os métodos devem ser chamados dentro de {@link Database#transaction}. */
public final class ReservationRepository {
    private static final String COLUMNS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, "
            + "check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund";

    private final Database db;

    public ReservationRepository(Database db) {
        this.db = db;
    }

    /** Serializa as criações de reserva, para que a checagem de quarto livre valha até o insert. */
    public void lockBookings() {
        run("SELECT pg_advisory_xact_lock(7070)", ps -> {
            ps.execute();
            return null;
        });
    }

    public void insert(Reservation r) {
        long id = run("INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, "
                + "check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS, ps -> {
            ps.setInt(1, r.roomNumber());
            ps.setString(2, r.guestName());
            ps.setString(3, r.guestEmail());
            ps.setString(4, r.guestCpf());
            ps.setDate(5, Date.valueOf(r.checkIn()));
            ps.setDate(6, Date.valueOf(r.checkOut()));
            ps.setInt(7, r.guests());
            ps.setString(8, r.ratePlan().name());
            ps.setInt(9, r.quote().nights());
            ps.setBigDecimal(10, r.quote().subtotal());
            ps.setBigDecimal(11, r.quote().discount());
            ps.setBigDecimal(12, r.quote().total());
            ps.setString(13, r.status().name());
            ps.setBigDecimal(14, r.amountPaid());
            ps.setBigDecimal(15, r.refund());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        });
        r.assignId(id);
    }

    public void update(Reservation r) {
        run("UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?", ps -> {
            ps.setString(1, r.status().name());
            ps.setBigDecimal(2, r.amountPaid());
            ps.setBigDecimal(3, r.refund());
            ps.setLong(4, r.id());
            ps.executeUpdate();
            return null;
        });
    }

    public Optional<Reservation> find(long id) {
        return query("SELECT " + COLUMNS + " FROM reservations WHERE id = ?", ps -> ps.setLong(1, id))
                .stream().findFirst();
    }

    /** Busca trancando a linha, para que operações simultâneas na mesma reserva se enfileirem. */
    public Optional<Reservation> findForUpdate(long id) {
        return query("SELECT " + COLUMNS + " FROM reservations WHERE id = ? FOR UPDATE", ps -> ps.setLong(1, id))
                .stream().findFirst();
    }

    /** Há reserva não cancelada do quarto em alguma das noites [checkIn, checkOut)? */
    public boolean isRoomBooked(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        return !query("SELECT " + COLUMNS + " FROM reservations WHERE room_number = ? AND status <> 'CANCELLED' "
                + "AND check_in < ? AND check_out > ? LIMIT 1", ps -> {
            ps.setInt(1, roomNumber);
            ps.setDate(2, Date.valueOf(checkOut));
            ps.setDate(3, Date.valueOf(checkIn));
        }).isEmpty();
    }

    /** Reservas pagas que ocupam um quarto na noite indicada, por número do quarto. */
    public List<Reservation> findPaidOccupying(LocalDate night) {
        return query("SELECT " + COLUMNS + " FROM reservations WHERE status IN ('CONFIRMED', 'CHECKED_IN', "
                + "'CHECKED_OUT') AND check_in <= ? AND check_out > ? ORDER BY room_number, id", ps -> {
            ps.setDate(1, Date.valueOf(night));
            ps.setDate(2, Date.valueOf(night));
        });
    }

    /** Reservas com entrada em [from, until) que deixaram dinheiro com a pousada. */
    public List<Reservation> findRetainingMoney(LocalDate from, LocalDate until) {
        return query("SELECT " + COLUMNS + " FROM reservations WHERE check_in >= ? AND check_in < ? "
                + "AND amount_paid - COALESCE(refund, 0) > 0 ORDER BY check_in, id", ps -> {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(until));
        });
    }

    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private interface Action<T> {
        T apply(PreparedStatement ps) throws SQLException;
    }

    private <T> T run(String sql, Action<T> action) {
        return run(sql, Statement.NO_GENERATED_KEYS, action);
    }

    private <T> T run(String sql, int keys, Action<T> action) {
        try (PreparedStatement ps = db.connection().prepareStatement(sql, keys)) {
            return action.apply(ps);
        } catch (SQLException e) {
            throw new UncheckedSqlException(e);
        }
    }

    private List<Reservation> query(String sql, Binder binder) {
        return run(sql, ps -> {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<Reservation> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        });
    }

    private Reservation map(ResultSet rs) throws SQLException {
        Quote quote = new Quote(rs.getInt("nights"), rs.getBigDecimal("subtotal"), rs.getBigDecimal("discount"),
                rs.getBigDecimal("total"));
        return Reservation.restore(rs.getLong("id"), rs.getInt("room_number"), rs.getString("guest_name"),
                rs.getString("guest_email"), rs.getString("guest_cpf"), rs.getDate("check_in").toLocalDate(),
                rs.getDate("check_out").toLocalDate(), rs.getInt("guests"),
                RatePlan.valueOf(rs.getString("rate_plan")), quote,
                ReservationStatus.valueOf(rs.getString("status")), rs.getBigDecimal("amount_paid"),
                rs.getBigDecimal("refund"));
    }
}
