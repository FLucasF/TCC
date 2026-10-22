package br.com.marealta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Acesso SQL; todos os métodos usam a conexão (transação) de quem chama. */
final class Repository {
    private Repository() {}

    private static final String RES_COLS = """
        id, room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name,
        guest_passport, guest_country, check_in, check_out, guests, rate_plan, nights,
        subtotal, discount, total, status, amount_paid, refund""";

    static List<Room> rooms(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number");
             ResultSet rs = ps.executeQuery()) {
            List<Room> out = new ArrayList<>();
            while (rs.next()) out.add(room(rs));
            return out;
        }
    }

    static Optional<Room> room(Connection c, int number, boolean lock) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?" + (lock ? " FOR UPDATE" : ""))) {
            ps.setInt(1, number);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(room(rs)) : Optional.empty();
            }
        }
    }

    private static Room room(ResultSet rs) throws SQLException {
        return new Room(rs.getInt("number"), RoomType.valueOf(rs.getString("type")),
                rs.getInt("capacity"), rs.getBigDecimal("nightly_rate"));
    }

    static boolean hasOverlap(Connection c, int room, LocalDate in, LocalDate out) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT 1 FROM reservations
                WHERE room_number = ? AND status <> 'CANCELLED' AND check_in < ? AND check_out > ? LIMIT 1""")) {
            ps.setInt(1, room);
            ps.setObject(2, out);
            ps.setObject(3, in);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    static long insert(Connection c, Reservation r) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name,
                  guest_passport, guest_country, check_in, check_out, guests, rate_plan, nights,
                  subtotal, discount, total, status, amount_paid, refund)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            ps.setInt(i++, r.roomNumber());
            ps.setString(i++, r.guestName());
            ps.setString(i++, r.guestEmail());
            String cpf = null, cnpj = null, company = null, passport = null, country = null;
            switch (r.identification()) {
                case Identification.Cpf id -> cpf = id.cpf();
                case Identification.Company id -> { cnpj = id.cnpj(); company = id.companyName(); }
                case Identification.Passport id -> { passport = id.passport(); country = id.country(); }
            }
            ps.setString(i++, cpf);
            ps.setString(i++, cnpj);
            ps.setString(i++, company);
            ps.setString(i++, passport);
            ps.setString(i++, country);
            ps.setObject(i++, r.checkIn());
            ps.setObject(i++, r.checkOut());
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
                return keys.getLong(1);
            }
        }
    }

    static Optional<Reservation> reservation(Connection c, long id, boolean lock) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT " + RES_COLS + " FROM reservations WHERE id = ?" + (lock ? " FOR UPDATE" : ""))) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(reservation(rs)) : Optional.empty();
            }
        }
    }

    /** Reservas que atendem a condição SQL (já com ORDER BY), com os parâmetros dados. */
    static List<Reservation> reservations(Connection c, String whereAndOrder, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT " + RES_COLS + " FROM reservations " + whereAndOrder)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                List<Reservation> out = new ArrayList<>();
                while (rs.next()) out.add(reservation(rs));
                return out;
            }
        }
    }

    private static Reservation reservation(ResultSet rs) throws SQLException {
        Identification id;
        if (rs.getString("guest_cpf") != null) {
            id = new Identification.Cpf(rs.getString("guest_cpf"));
        } else if (rs.getString("company_cnpj") != null) {
            id = new Identification.Company(rs.getString("company_cnpj"), rs.getString("company_name"));
        } else {
            id = new Identification.Passport(rs.getString("guest_passport"), rs.getString("guest_country"));
        }
        return new Reservation(rs.getLong("id"), rs.getInt("room_number"), rs.getString("guest_name"),
                rs.getString("guest_email"), id,
                rs.getObject("check_in", LocalDate.class), rs.getObject("check_out", LocalDate.class),
                rs.getInt("guests"), RatePlan.valueOf(rs.getString("rate_plan")), rs.getInt("nights"),
                rs.getBigDecimal("subtotal"), rs.getBigDecimal("discount"), rs.getBigDecimal("total"),
                Status.valueOf(rs.getString("status")), rs.getBigDecimal("amount_paid"), rs.getBigDecimal("refund"));
    }

    static void updateState(Connection c, long id, Status status, java.math.BigDecimal amountPaid,
                            java.math.BigDecimal refund) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setBigDecimal(2, amountPaid);
            ps.setBigDecimal(3, refund);
            ps.setLong(4, id);
            ps.executeUpdate();
        }
    }

    static void insertMessage(Connection c, String channel, String to, String subject, String body) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO messages (channel, recipient, subject, body) VALUES (?,?,?,?)")) {
            ps.setString(1, channel);
            ps.setString(2, to);
            ps.setString(3, subject);
            ps.setString(4, body);
            ps.executeUpdate();
        }
    }

    static List<Message> messages(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            List<Message> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new Message(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)));
            }
            return out;
        }
    }
}
