package br.com.marealta;

import java.math.BigDecimal;
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

/** Acesso às reservas; todas as operações rodam na transação da conexão recebida. */
public class ReservationRepository {
    private static final String COLUMNS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, "
            + "check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund";

    /** Serializa as reservas concorrentes do mesmo quarto até o fim da transação. */
    public void lockRoom(Connection c, int roomNumber) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
            s.setLong(1, roomNumber);
            s.execute();
        }
    }

    /** Há reserva não cancelada do quarto em alguma noite de [checkIn, checkOut)? */
    public boolean isRoomBooked(Connection c, int roomNumber, LocalDate checkIn, LocalDate checkOut)
            throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT 1 FROM reservations WHERE room_number = ? "
                + "AND status <> 'CANCELLED' AND check_in < ? AND check_out > ? LIMIT 1")) {
            s.setInt(1, roomNumber);
            s.setDate(2, Date.valueOf(checkOut));
            s.setDate(3, Date.valueOf(checkIn));
            try (ResultSet rs = s.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Reservation insert(Connection c, Reservation r) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("INSERT INTO reservations (room_number, guest_name, "
                + "guest_email, guest_cpf, check_in, check_out, guests, rate_plan, nights, subtotal, discount, "
                + "total, status, amount_paid, refund) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            s.setInt(1, r.roomNumber());
            s.setString(2, r.guestName());
            s.setString(3, r.guestEmail());
            s.setString(4, r.guestCpf());
            s.setDate(5, Date.valueOf(r.checkIn()));
            s.setDate(6, Date.valueOf(r.checkOut()));
            s.setInt(7, r.guests());
            s.setString(8, r.ratePlan().name());
            s.setInt(9, r.nights());
            s.setBigDecimal(10, r.subtotal());
            s.setBigDecimal(11, r.discount());
            s.setBigDecimal(12, r.total());
            s.setString(13, r.status().name());
            s.setBigDecimal(14, r.amountPaid());
            s.setBigDecimal(15, r.refund());
            s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) {
                keys.next();
                return r.withId(keys.getLong("id"));
            }
        }
    }

    public void update(Connection c, Reservation r) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(
                "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
            s.setString(1, r.status().name());
            s.setBigDecimal(2, r.amountPaid());
            s.setBigDecimal(3, r.refund());
            s.setLong(4, r.id());
            s.executeUpdate();
        }
    }

    /** Busca e trava a reserva até o fim da transação. */
    public Optional<Reservation> findForUpdate(Connection c, long id) throws SQLException {
        return query(c, "SELECT " + COLUMNS + " FROM reservations WHERE id = ? FOR UPDATE", id).stream().findFirst();
    }

    public Optional<Reservation> find(Connection c, long id) throws SQLException {
        return query(c, "SELECT " + COLUMNS + " FROM reservations WHERE id = ?", id).stream().findFirst();
    }

    /** Reservas pagas e não canceladas que ocupam um quarto na noite, por número do quarto. */
    public List<Reservation> occupying(Connection c, LocalDate night) throws SQLException {
        return query(c, "SELECT " + COLUMNS + " FROM reservations WHERE status IN ('CONFIRMED','CHECKED_IN',"
                + "'CHECKED_OUT') AND check_in <= ? AND check_out > ? ORDER BY room_number",
                Date.valueOf(night), Date.valueOf(night));
    }

    /** Reservas que começam em [from, to) e deixaram dinheiro com a pousada. */
    public List<Reservation> retainingStartingBetween(Connection c, LocalDate from, LocalDate to)
            throws SQLException {
        return query(c, "SELECT " + COLUMNS + " FROM reservations WHERE check_in >= ? AND check_in < ? "
                + "AND amount_paid - COALESCE(refund, 0) > 0 ORDER BY check_in, id",
                Date.valueOf(from), Date.valueOf(to));
    }

    private List<Reservation> query(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) s.setObject(i + 1, params[i]);
            try (ResultSet rs = s.executeQuery()) {
                List<Reservation> result = new ArrayList<>();
                while (rs.next()) result.add(read(rs));
                return result;
            }
        }
    }

    private Reservation read(ResultSet rs) throws SQLException {
        BigDecimal refund = rs.getBigDecimal("refund");
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
                refund);
    }
}
