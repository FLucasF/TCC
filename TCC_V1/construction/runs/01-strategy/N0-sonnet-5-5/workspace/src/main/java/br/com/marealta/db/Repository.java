package br.com.marealta.db;

import br.com.marealta.model.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Acesso às tabelas; todos os métodos usam a conexão (transação) de quem chama. */
public final class Repository {
    private Repository() {}

    private static final String RES_COLS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
            + "guests, rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid, refund";

    public static List<Room> rooms(Connection c) throws SQLException {
        List<Room> out = new ArrayList<>();
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number")) {
            while (rs.next()) out.add(room(rs));
        }
        return out;
    }

    /** Busca o quarto travando a linha, para serializar reservas concorrentes do mesmo quarto. */
    public static Optional<Room> lockRoom(Connection c, int number) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ? FOR UPDATE")) {
            ps.setInt(1, number);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(room(rs)) : Optional.empty();
            }
        }
    }

    public static Optional<Room> room(Connection c, int number) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?")) {
            ps.setInt(1, number);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(room(rs)) : Optional.empty();
            }
        }
    }

    public static boolean hasConflict(Connection c, int room, LocalDate in, LocalDate out) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM reservations WHERE room_number = ? AND status <> 'CANCELLED' "
                        + "AND check_in < ? AND check_out > ? LIMIT 1")) {
            ps.setInt(1, room);
            ps.setDate(2, Date.valueOf(out));
            ps.setDate(3, Date.valueOf(in));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static long insert(Connection c, Reservation r) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, "
                        + "rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id")) {
            int i = 1;
            ps.setInt(i++, r.roomNumber());
            ps.setString(i++, r.guestName());
            ps.setString(i++, r.guestEmail());
            ps.setString(i++, r.guestCpf());
            ps.setDate(i++, Date.valueOf(r.checkIn()));
            ps.setDate(i++, Date.valueOf(r.checkOut()));
            ps.setInt(i++, r.guests());
            ps.setString(i++, r.ratePlan());
            ps.setString(i++, r.companyCode());
            ps.setInt(i++, r.nights());
            ps.setBigDecimal(i++, r.subtotal());
            ps.setBigDecimal(i++, r.discount());
            ps.setBigDecimal(i++, r.total());
            ps.setString(i++, r.status().name());
            ps.setBigDecimal(i, r.amountPaid());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public static Optional<Reservation> reservation(Connection c, long id, boolean lock) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT " + RES_COLS + " FROM reservations WHERE id = ?" + (lock ? " FOR UPDATE" : ""))) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(reservation(rs)) : Optional.empty();
            }
        }
    }

    public static void updateState(Connection c, long id, ReservationStatus status, BigDecimal paid, BigDecimal refund)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setBigDecimal(2, paid);
            ps.setBigDecimal(3, refund);
            ps.setLong(4, id);
            ps.executeUpdate();
        }
    }

    public static void insertMessage(Connection c, String channel, String to, String subject, String body)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO messages (channel, recipient, subject, body) VALUES (?,?,?,?)")) {
            ps.setString(1, channel);
            ps.setString(2, to);
            ps.setString(3, subject);
            ps.setString(4, body);
            ps.executeUpdate();
        }
    }

    public static List<Message> messages(Connection c) throws SQLException {
        List<Message> out = new ArrayList<>();
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, channel, recipient, subject, body FROM messages ORDER BY id")) {
            while (rs.next()) {
                out.add(new Message(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)));
            }
        }
        return out;
    }

    private static Room room(ResultSet rs) throws SQLException {
        return new Room(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getBigDecimal(4));
    }

    private static Reservation reservation(ResultSet rs) throws SQLException {
        return new Reservation(rs.getLong(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getString(5),
                rs.getDate(6).toLocalDate(), rs.getDate(7).toLocalDate(), rs.getInt(8), rs.getString(9),
                rs.getString(10), rs.getInt(11), rs.getBigDecimal(12), rs.getBigDecimal(13), rs.getBigDecimal(14),
                ReservationStatus.valueOf(rs.getString(15)), rs.getBigDecimal(16), rs.getBigDecimal(17));
    }
}
