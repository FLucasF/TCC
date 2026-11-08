package br.com.marealta;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Acesso JDBC; todos os métodos usam a conexão (transação) recebida. */
public final class Repository {
    private static final String RES_COLS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, "
            + "rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid, refund";

    // ---- quartos
    public List<Room> rooms(Connection c) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(
                "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number")) {
            List<Room> out = new ArrayList<>();
            while (rs.next()) out.add(room(rs));
            return out;
        }
    }

    public Optional<Room> room(Connection c, int number, boolean lock) throws SQLException {
        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?" + (lock ? " FOR UPDATE" : "");
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, number);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(room(rs)) : Optional.empty();
            }
        }
    }

    private static Room room(ResultSet rs) throws SQLException {
        return new Room(rs.getInt(1), RoomType.valueOf(rs.getString(2)), rs.getInt(3), rs.getBigDecimal(4));
    }

    // ---- reservas
    public boolean hasOverlap(Connection c, int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM reservations WHERE room_number = ? AND status <> 'CANCELLED' AND check_in < ? AND ? < check_out LIMIT 1")) {
            ps.setInt(1, roomNumber);
            ps.setObject(2, checkOut);
            ps.setObject(3, checkIn);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Reservation insert(Connection c, Reservation r) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan, "
                        + "company_code, nights, subtotal, discount, total, status, amount_paid) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) "
                        + "RETURNING " + RES_COLS)) {
            int i = 1;
            ps.setInt(i++, r.roomNumber());
            ps.setString(i++, r.guestName());
            ps.setString(i++, r.guestEmail());
            ps.setString(i++, r.guestCpf());
            ps.setObject(i++, r.checkIn());
            ps.setObject(i++, r.checkOut());
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
                return reservation(rs);
            }
        }
    }

    public Optional<Reservation> find(Connection c, long id, boolean lock) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT " + RES_COLS + " FROM reservations WHERE id = ?" + (lock ? " FOR UPDATE" : ""))) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(reservation(rs)) : Optional.empty();
            }
        }
    }

    public Reservation update(Connection c, long id, ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ? RETURNING " + RES_COLS)) {
            ps.setString(1, status.name());
            ps.setBigDecimal(2, amountPaid);
            ps.setBigDecimal(3, refund);
            ps.setLong(4, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return reservation(rs);
            }
        }
    }

    /** Reservas pagas (confirmada, check-in, check-out) que ocupam o quarto na noite indicada. */
    public List<Reservation> occupying(Connection c, LocalDate night) throws SQLException {
        return list(c, "SELECT " + RES_COLS + " FROM reservations WHERE status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT') "
                + "AND check_in <= ? AND ? < check_out ORDER BY room_number, id", night, night);
    }

    /** Reservas que começam no período [from, to) e ainda têm dinheiro com a pousada. */
    public List<Reservation> revenue(Connection c, LocalDate from, LocalDate to) throws SQLException {
        return list(c, "SELECT " + RES_COLS + " FROM reservations WHERE check_in >= ? AND check_in < ? "
                + "AND amount_paid - COALESCE(refund, 0) > 0 ORDER BY check_in, id", from, to);
    }

    private List<Reservation> list(Connection c, String sql, LocalDate a, LocalDate b) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, a);
            ps.setObject(2, b);
            try (ResultSet rs = ps.executeQuery()) {
                List<Reservation> out = new ArrayList<>();
                while (rs.next()) out.add(reservation(rs));
                return out;
            }
        }
    }

    private static Reservation reservation(ResultSet rs) throws SQLException {
        return new Reservation(
                rs.getLong("id"), rs.getInt("room_number"), rs.getString("guest_name"), rs.getString("guest_email"),
                rs.getString("guest_cpf"), rs.getObject("check_in", LocalDate.class), rs.getObject("check_out", LocalDate.class),
                rs.getInt("guests"), rs.getString("rate_plan"), rs.getString("company_code"), rs.getInt("nights"),
                rs.getBigDecimal("subtotal"), rs.getBigDecimal("discount"), rs.getBigDecimal("total"),
                ReservationStatus.valueOf(rs.getString("status")), rs.getBigDecimal("amount_paid"), rs.getBigDecimal("refund"));
    }

    // ---- mensagens
    public void insertMessage(Connection c, Message m) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO messages (channel, recipient, subject, body) VALUES (?,?,?,?)")) {
            ps.setString(1, m.channel());
            ps.setString(2, m.to());
            ps.setString(3, m.subject());
            ps.setString(4, m.body());
            ps.executeUpdate();
        }
    }

    public List<Message> messages(Connection c) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(
                "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id")) {
            List<Message> out = new ArrayList<>();
            while (rs.next()) out.add(new Message(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)));
            return out;
        }
    }
}
