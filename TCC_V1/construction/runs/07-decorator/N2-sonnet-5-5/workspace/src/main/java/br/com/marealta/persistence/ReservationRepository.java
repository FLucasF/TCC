package br.com.marealta.persistence;

import br.com.marealta.domain.Pricing;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ReservationRepository {
    private static final String COLUMNS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,"
            + " guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund";

    private final Database database;

    public ReservationRepository(Database database) { this.database = database; }

    /** Grava a reserva se o quarto estiver livre em todas as noites; devolve false se não estiver. */
    public boolean insertIfAvailable(Reservation r) {
        try (Connection c = database.open()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement lock = c.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
                    lock.setLong(1, r.roomNumber());
                    lock.execute();
                }
                try (PreparedStatement overlap = c.prepareStatement(
                        "SELECT 1 FROM reservations WHERE room_number = ? AND status <> 'CANCELLED'"
                                + " AND check_in < ? AND check_out > ?")) {
                    overlap.setInt(1, r.roomNumber());
                    overlap.setObject(2, r.checkOut());
                    overlap.setObject(3, r.checkIn());
                    try (ResultSet rs = overlap.executeQuery()) {
                        if (rs.next()) {
                            c.rollback();
                            return false;
                        }
                    }
                }
                try (PreparedStatement insert = c.prepareStatement(
                        "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in,"
                                + " check_out, guests, rate_plan, nights, subtotal, discount, total, status,"
                                + " amount_paid, refund) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    insert.setInt(1, r.roomNumber());
                    insert.setString(2, r.guestName());
                    insert.setString(3, r.guestEmail());
                    insert.setString(4, r.guestCpf());
                    insert.setObject(5, r.checkIn());
                    insert.setObject(6, r.checkOut());
                    insert.setInt(7, r.guests());
                    insert.setString(8, r.ratePlan().name());
                    insert.setInt(9, r.nights());
                    insert.setBigDecimal(10, r.subtotal());
                    insert.setBigDecimal(11, r.discount());
                    insert.setBigDecimal(12, r.total());
                    insert.setString(13, r.status().name());
                    insert.setBigDecimal(14, r.amountPaid());
                    insert.setBigDecimal(15, r.refund());
                    insert.executeUpdate();
                    try (ResultSet keys = insert.getGeneratedKeys()) {
                        keys.next();
                        r.assignId(keys.getLong(1));
                    }
                }
                c.commit();
                return true;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public Optional<Reservation> find(long id) {
        List<Reservation> found = query("SELECT " + COLUMNS + " FROM reservations WHERE id = ?", ps -> ps.setLong(1, id));
        return found.stream().findFirst();
    }

    public void update(Reservation r) {
        try (Connection c = database.open();
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

    /** Reservas que ocupam um quarto na noite indicada (entra nela, não sai nela), qualquer situação. */
    public List<Reservation> findOccupying(LocalDate night) {
        return query("SELECT " + COLUMNS + " FROM reservations WHERE check_in <= ? AND check_out > ? ORDER BY id",
                ps -> {
                    ps.setObject(1, night);
                    ps.setObject(2, night);
                });
    }

    /** Reservas com entrada no intervalo [from, until). */
    public List<Reservation> findCheckingInBetween(LocalDate from, LocalDate until) {
        return query("SELECT " + COLUMNS + " FROM reservations WHERE check_in >= ? AND check_in < ? ORDER BY id",
                ps -> {
                    ps.setObject(1, from);
                    ps.setObject(2, until);
                });
    }

    private interface Binder { void bind(PreparedStatement ps) throws SQLException; }

    private List<Reservation> query(String sql, Binder binder) {
        try (Connection c = database.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<Reservation> result = new ArrayList<>();
                while (rs.next()) result.add(read(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Reservation read(ResultSet rs) throws SQLException {
        BigDecimal refund = rs.getBigDecimal("refund");
        Pricing.Quote quote = new Pricing.Quote(rs.getInt("nights"), rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("discount"), rs.getBigDecimal("total"));
        return new Reservation(rs.getLong("id"), rs.getInt("room_number"), rs.getString("guest_name"),
                rs.getString("guest_email"), rs.getString("guest_cpf"),
                rs.getObject("check_in", LocalDate.class), rs.getObject("check_out", LocalDate.class),
                rs.getInt("guests"), RatePlan.valueOf(rs.getString("rate_plan")), quote,
                ReservationStatus.valueOf(rs.getString("status")), rs.getBigDecimal("amount_paid"), refund);
    }
}
