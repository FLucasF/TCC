package br.com.marealta.persistence;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Quote;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.domain.Rooms;
import br.com.marealta.plan.RatePlans;

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
    private static final String COLUMNS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
            + "guests, rate_plan, nights, subtotal, discount, status, amount_paid, refund";

    /** Serializa as reservas de um mesmo quarto até o fim da transação. */
    public void lockRoom(Connection c, int roomNumber) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
            ps.setLong(1, 1_000_000L + roomNumber);
            ps.execute();
        }
    }

    public boolean isRoomBooked(Connection c, int roomNumber, LocalDate checkIn, LocalDate checkOut)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM reservations WHERE room_number = ? "
                + "AND status <> 'CANCELLED' AND check_in < ? AND check_out > ? LIMIT 1")) {
            ps.setInt(1, roomNumber);
            ps.setDate(2, Date.valueOf(checkOut));
            ps.setDate(3, Date.valueOf(checkIn));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public long insert(Connection c, Reservation r) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO reservations (room_number, guest_name, "
                + "guest_email, guest_cpf, check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, "
                + "status, amount_paid, refund) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.room().number());
            ps.setString(2, r.guestName());
            ps.setString(3, r.guestEmail());
            ps.setString(4, r.guestCpf());
            ps.setDate(5, Date.valueOf(r.checkInDate()));
            ps.setDate(6, Date.valueOf(r.checkOutDate()));
            ps.setInt(7, r.guests());
            ps.setString(8, r.ratePlan().code());
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
                return keys.getLong(1);
            }
        }
    }

    public Optional<Reservation> findById(Connection c, long id, boolean forUpdate) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM reservations WHERE id = ?"
                + (forUpdate ? " FOR UPDATE" : ""))) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(read(rs)) : Optional.empty();
            }
        }
    }

    public void save(Connection c, Reservation r) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
            ps.setString(1, r.status().name());
            ps.setBigDecimal(2, r.amountPaid());
            ps.setBigDecimal(3, r.refund());
            ps.setLong(4, r.id());
            ps.executeUpdate();
        }
    }

    /** Reservas pagas que ocupam um quarto na noite de {@code night}, por número do quarto. */
    public List<Reservation> findPaidOccupying(Connection c, LocalDate night) throws SQLException {
        return query(c, "WHERE status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT') AND check_in <= ? AND check_out > ? "
                + "ORDER BY room_number, id", Date.valueOf(night), Date.valueOf(night));
    }

    /** Reservas com entrada entre as duas datas (inclusive), por data de entrada e número. */
    public List<Reservation> findCheckingInBetween(Connection c, LocalDate from, LocalDate to) throws SQLException {
        return query(c, "WHERE check_in >= ? AND check_in <= ? ORDER BY check_in, id",
                Date.valueOf(from), Date.valueOf(to));
    }

    private List<Reservation> query(Connection c, String clause, Date... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM reservations " + clause)) {
            for (int i = 0; i < params.length; i++) {
                ps.setDate(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Reservation> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(read(rs));
                }
                return list;
            }
        }
    }

    private Reservation read(ResultSet rs) throws SQLException {
        Quote quote = new Quote(rs.getInt("nights"), rs.getBigDecimal("subtotal"), rs.getBigDecimal("discount"));
        return new Reservation(rs.getLong("id"), Rooms.find(rs.getInt("room_number")).orElseThrow(),
                rs.getString("guest_name"), rs.getString("guest_email"), rs.getString("guest_cpf"),
                rs.getDate("check_in").toLocalDate(), rs.getDate("check_out").toLocalDate(), rs.getInt("guests"),
                RatePlans.byCode(rs.getString("rate_plan")), quote,
                ReservationStatus.valueOf(rs.getString("status")),
                Money.round(rs.getBigDecimal("amount_paid")), rs.getBigDecimal("refund") == null ? null
                        : Money.round(rs.getBigDecimal("refund")));
    }
}
