package pousada.persistence;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

import pousada.domain.RatePlan;
import pousada.domain.Reservation;
import pousada.domain.ReservationStatus;

public final class ReservationRepository {
    private final Database db;

    public ReservationRepository(Database db) {
        this.db = db;
    }

    /** Grava a reserva se o quarto estiver livre em todas as noites; vazio se não estiver. */
    public Optional<Reservation> insertIfRoomFree(Reservation draft) {
        return db.transaction(c -> {
            try (var lock = c.prepareStatement("SELECT 1 FROM rooms WHERE number = ? FOR UPDATE")) {
                lock.setInt(1, draft.roomNumber());
                lock.executeQuery().close();
            }
            try (var overlap = c.prepareStatement("""
                    SELECT 1 FROM reservations
                    WHERE room_number = ? AND status <> 'CANCELLED' AND check_in < ? AND check_out > ?
                    LIMIT 1""")) {
                overlap.setInt(1, draft.roomNumber());
                overlap.setObject(2, draft.checkOut());
                overlap.setObject(3, draft.checkIn());
                try (var rs = overlap.executeQuery()) {
                    if (rs.next()) {
                        return Optional.<Reservation>empty();
                    }
                }
            }
            return Optional.of(insert(c, draft));
        });
    }

    public Optional<Reservation> find(long id) {
        return db.transaction(c -> select(c, id, false));
    }

    /** Aplica a mudança com a linha travada, para duas operações na mesma reserva não se atropelarem. */
    public Optional<Reservation> update(long id, UnaryOperator<Reservation> change) {
        return db.transaction(c -> {
            Optional<Reservation> current = select(c, id, true);
            if (current.isEmpty()) {
                return current;
            }
            Reservation updated = change.apply(current.get());
            try (var ps = c.prepareStatement(
                    "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
                ps.setString(1, updated.status().name());
                ps.setBigDecimal(2, updated.amountPaid());
                ps.setBigDecimal(3, updated.refund());
                ps.setLong(4, id);
                ps.executeUpdate();
            }
            return Optional.of(updated);
        });
    }

    /** Reservas pagas que ocupam um quarto na noite: entram na noite da entrada, saem no dia da saída. */
    public List<Reservation> occupying(LocalDate night) {
        return query("""
                SELECT * FROM reservations
                WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') AND check_in <= ? AND check_out > ?
                ORDER BY room_number, id""", night, night);
    }

    /** Reservas com entrada no mês e dinheiro retido pela pousada (pago menos devolvido). */
    public List<Reservation> keptMoneyStartingIn(YearMonth month) {
        return query("""
                SELECT * FROM reservations
                WHERE check_in >= ? AND check_in < ? AND amount_paid - COALESCE(refund, 0) > 0
                ORDER BY check_in, id""", month.atDay(1), month.plusMonths(1).atDay(1));
    }

    private List<Reservation> query(String sql, LocalDate first, LocalDate second) {
        return db.transaction(c -> {
            try (var ps = c.prepareStatement(sql)) {
                ps.setObject(1, first);
                ps.setObject(2, second);
                try (var rs = ps.executeQuery()) {
                    List<Reservation> list = new ArrayList<>();
                    while (rs.next()) {
                        list.add(map(rs));
                    }
                    return list;
                }
            }
        });
    }

    private static Optional<Reservation> select(Connection c, long id, boolean lock) throws SQLException {
        try (var ps = c.prepareStatement("SELECT * FROM reservations WHERE id = ?" + (lock ? " FOR UPDATE" : ""))) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.<Reservation>empty();
            }
        }
    }

    private static Reservation insert(Connection c, Reservation r) throws SQLException {
        try (var ps = c.prepareStatement("""
                INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                    guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
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
            try (var keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Reservation(keys.getLong(1), r.roomNumber(), r.guestName(), r.guestEmail(),
                        r.guestCpf(), r.checkIn(), r.checkOut(), r.guests(), r.ratePlan(), r.nights(),
                        r.subtotal(), r.discount(), r.total(), r.status(), r.amountPaid(), r.refund());
            }
        }
    }

    private static Reservation map(ResultSet rs) throws SQLException {
        return new Reservation(rs.getLong("id"), rs.getInt("room_number"), rs.getString("guest_name"),
                rs.getString("guest_email"), rs.getString("guest_cpf"),
                rs.getObject("check_in", LocalDate.class), rs.getObject("check_out", LocalDate.class),
                rs.getInt("guests"), RatePlan.valueOf(rs.getString("rate_plan")), rs.getInt("nights"),
                rs.getBigDecimal("subtotal"), rs.getBigDecimal("discount"), rs.getBigDecimal("total"),
                ReservationStatus.valueOf(rs.getString("status")), rs.getBigDecimal("amount_paid"),
                rs.getBigDecimal("refund"));
    }
}
