package pousada;

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

/** SQL access for reservations and messages. Callers provide the transaction. */
public final class Store {
    private Store() {}

    private static final String COLUMNS = """
            id, room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name,
            guest_passport, guest_country, check_in, check_out, guests, rate_plan, nights,
            subtotal, discount, total, status, amount_paid, refund""";

    /** Serializes bookings of one room within the transaction. */
    public static void lockRoom(Connection c, int roomNumber) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
            ps.setLong(1, roomNumber);
            ps.execute();
        }
    }

    public static boolean isRoomBusy(Connection c, int roomNumber, LocalDate checkIn, LocalDate checkOut)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT 1 FROM reservations
                WHERE room_number = ? AND status <> 'CANCELLED' AND check_in < ? AND check_out > ?
                LIMIT 1""")) {
            ps.setInt(1, roomNumber);
            ps.setDate(2, Date.valueOf(checkOut));
            ps.setDate(3, Date.valueOf(checkIn));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static Reservation insert(Connection c, Reservation r) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, company_cnpj,
                    company_name, guest_passport, guest_country, check_in, check_out, guests, rate_plan,
                    nights, subtotal, discount, total, status, amount_paid, refund)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            ps.setInt(i++, r.roomNumber());
            ps.setString(i++, r.guestName());
            ps.setString(i++, r.guestEmail());
            ps.setString(i++, r.identity().cpf());
            ps.setString(i++, r.identity().cnpj());
            ps.setString(i++, r.identity().companyName());
            ps.setString(i++, r.identity().passport());
            ps.setString(i++, r.identity().country());
            ps.setDate(i++, Date.valueOf(r.checkIn()));
            ps.setDate(i++, Date.valueOf(r.checkOut()));
            ps.setInt(i++, r.guests());
            ps.setString(i++, r.ratePlan().name());
            ps.setInt(i++, r.nights());
            ps.setBigDecimal(i++, r.subtotal());
            ps.setBigDecimal(i++, r.discount());
            ps.setBigDecimal(i++, r.total());
            ps.setString(i++, r.status().name());
            ps.setBigDecimal(i++, r.amountPaid());
            ps.setBigDecimal(i, r.refund());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return find(c, keys.getLong(1), false).orElseThrow();
            }
        }
    }

    public static Optional<Reservation> find(Connection c, long id, boolean forUpdate) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT " + COLUMNS + " FROM reservations WHERE id = ?" + (forUpdate ? " FOR UPDATE" : ""))) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(read(rs)) : Optional.empty();
            }
        }
    }

    public static void updateState(Connection c, Reservation r) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
            ps.setString(1, r.status().name());
            ps.setBigDecimal(2, r.amountPaid());
            ps.setBigDecimal(3, r.refund());
            ps.setLong(4, r.id());
            ps.executeUpdate();
        }
    }

    /** Paid reservations occupying a room on the given night. */
    public static List<Reservation> occupying(Connection c, LocalDate night) throws SQLException {
        return query(c, "WHERE status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT') AND check_in <= ? AND check_out > ? "
                + "ORDER BY room_number, id", night, night);
    }

    /** Reservations starting within [from, to) that kept money with the inn. */
    public static List<Reservation> startingBetween(Connection c, LocalDate from, LocalDate to) throws SQLException {
        return query(c, "WHERE check_in >= ? AND check_in < ? AND amount_paid - COALESCE(refund, 0) > 0 "
                + "ORDER BY check_in, id", from, to);
    }

    private static List<Reservation> query(Connection c, String tail, LocalDate a, LocalDate b) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM reservations " + tail)) {
            ps.setDate(1, Date.valueOf(a));
            ps.setDate(2, Date.valueOf(b));
            List<Reservation> out = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(read(rs));
            }
            return out;
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
        try (Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT id, channel, recipient, subject, body FROM messages ORDER BY id")) {
            List<Message> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new Message(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)));
            }
            return out;
        }
    }

    private static Reservation read(ResultSet rs) throws SQLException {
        GuestIdentity identity;
        if (rs.getString("guest_cpf") != null) {
            identity = new GuestIdentity.Individual(rs.getString("guest_cpf"));
        } else if (rs.getString("company_cnpj") != null) {
            identity = new GuestIdentity.Company(rs.getString("company_cnpj"), rs.getString("company_name"));
        } else {
            identity = new GuestIdentity.Foreigner(rs.getString("guest_passport"), rs.getString("guest_country"));
        }
        return new Reservation(
                rs.getLong("id"),
                rs.getInt("room_number"),
                rs.getString("guest_name"),
                rs.getString("guest_email"),
                identity,
                rs.getDate("check_in").toLocalDate(),
                rs.getDate("check_out").toLocalDate(),
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
