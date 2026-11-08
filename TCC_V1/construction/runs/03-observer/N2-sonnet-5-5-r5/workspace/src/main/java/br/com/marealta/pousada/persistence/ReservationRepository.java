package br.com.marealta.pousada.persistence;

import br.com.marealta.pousada.domain.ErrorCode;
import br.com.marealta.pousada.domain.PousadaException;
import br.com.marealta.pousada.domain.RatePlan;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.ReservationStatus;
import br.com.marealta.pousada.domain.Room;

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

public class ReservationRepository {
    private static final String SELECT = """
            SELECT r.*, rm.type AS room_type FROM reservations r
            JOIN rooms rm ON rm.number = r.room_number
            """;

    private final Database db;

    public ReservationRepository(Database db) {
        this.db = db;
    }

    /** Grava a reserva nova se o quarto estiver livre em todas as noites; senão, ROOM_UNAVAILABLE. */
    public void insertIfAvailable(Reservation r) {
        try (Connection c = db.connect()) {
            c.setAutoCommit(false);
            try {
                // Trava o quarto para que duas reservas simultâneas não passem juntas pela verificação.
                try (PreparedStatement lock = c.prepareStatement("SELECT 1 FROM rooms WHERE number = ? FOR UPDATE")) {
                    lock.setInt(1, r.room().number());
                    lock.executeQuery().close();
                }
                try (PreparedStatement overlap = c.prepareStatement("""
                        SELECT 1 FROM reservations
                        WHERE room_number = ? AND status <> 'CANCELLED' AND check_in < ? AND check_out > ?
                        LIMIT 1""")) {
                    overlap.setInt(1, r.room().number());
                    overlap.setDate(2, Date.valueOf(r.checkOut()));
                    overlap.setDate(3, Date.valueOf(r.checkIn()));
                    try (ResultSet rs = overlap.executeQuery()) {
                        if (rs.next()) {
                            throw new PousadaException(ErrorCode.ROOM_UNAVAILABLE,
                                    "O quarto " + r.room().number() + " já está reservado em alguma das noites pedidas");
                        }
                    }
                }
                try (PreparedStatement ps = c.prepareStatement("""
                        INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                            guests, rate_plan, subtotal, discount, total, status, amount_paid, refund)
                        VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, r.room().number());
                    ps.setString(2, r.guestName());
                    ps.setString(3, r.guestEmail());
                    ps.setString(4, r.guestCpf());
                    ps.setDate(5, Date.valueOf(r.checkIn()));
                    ps.setDate(6, Date.valueOf(r.checkOut()));
                    ps.setInt(7, r.guests());
                    ps.setString(8, r.ratePlan().name());
                    ps.setBigDecimal(9, r.subtotal());
                    ps.setBigDecimal(10, r.discount());
                    ps.setBigDecimal(11, r.total());
                    ps.setString(12, r.status().name());
                    ps.setBigDecimal(13, r.amountPaid());
                    ps.setBigDecimal(14, r.refund());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        r.assignId(keys.getLong(1));
                    }
                }
                c.commit();
            } catch (RuntimeException | SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Grava a mudança só se a reserva ainda estiver na situação em que foi lida. */
    public boolean update(Reservation r, ReservationStatus expectedStatus) {
        try (Connection c = db.connect();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ? AND status = ?")) {
            ps.setString(1, r.status().name());
            ps.setBigDecimal(2, r.amountPaid());
            ps.setBigDecimal(3, r.refund());
            ps.setLong(4, r.id());
            ps.setString(5, expectedStatus.name());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public Optional<Reservation> findById(long id) {
        List<Reservation> found = query(SELECT + "WHERE r.id = ?", ps -> ps.setLong(1, id));
        return found.stream().findFirst();
    }

    /** Reservas pagas (não pendentes nem canceladas) que ocupam um quarto na noite dada. */
    public List<Reservation> findPaidOccupying(LocalDate night) {
        return query(SELECT + "WHERE r.status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT') "
                + "AND r.check_in <= ? AND r.check_out > ? ORDER BY r.room_number, r.id", ps -> {
            ps.setDate(1, Date.valueOf(night));
            ps.setDate(2, Date.valueOf(night));
        });
    }

    /** Reservas que começam no intervalo [from, to) e deixaram dinheiro com a pousada. */
    public List<Reservation> findKeptMoneyStartingBetween(LocalDate from, LocalDate to) {
        return query(SELECT + "WHERE r.check_in >= ? AND r.check_in < ? "
                + "AND r.amount_paid - COALESCE(r.refund, 0) > 0 ORDER BY r.check_in, r.id", ps -> {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
        });
    }

    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private List<Reservation> query(String sql, Binder binder) {
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
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
        Room room = new Room(rs.getInt("room_number"),
                br.com.marealta.pousada.domain.RoomType.valueOf(rs.getString("room_type")));
        return new Reservation(rs.getLong("id"), room, rs.getString("guest_name"), rs.getString("guest_email"),
                rs.getString("guest_cpf"), rs.getDate("check_in").toLocalDate(), rs.getDate("check_out").toLocalDate(),
                rs.getInt("guests"), RatePlan.valueOf(rs.getString("rate_plan")),
                rs.getBigDecimal("subtotal"), rs.getBigDecimal("discount"), rs.getBigDecimal("total"),
                ReservationStatus.valueOf(rs.getString("status")), rs.getBigDecimal("amount_paid"),
                rs.getBigDecimal("refund"));
    }
}
