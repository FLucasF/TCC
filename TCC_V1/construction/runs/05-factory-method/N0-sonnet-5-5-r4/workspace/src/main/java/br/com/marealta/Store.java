package br.com.marealta;

import br.com.marealta.Model.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

/** Acesso ao banco. Os métodos que recebem Connection participam da transação do chamador. */
public final class Store {
    private final DataSource ds;

    public Store(DataSource ds) {
        this.ds = ds;
    }

    @FunctionalInterface
    public interface Work<T> {
        T run(Connection c) throws SQLException;
    }

    public <T> T tx(Work<T> work) {
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (Throwable t) {
                c.rollback();
                throw t;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---- quartos

    public List<Room> rooms(Connection c) throws SQLException {
        List<Room> out = new ArrayList<>();
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number")) {
            while (rs.next()) out.add(room(rs));
        }
        return out;
    }

    /** Busca o quarto travando a linha, o que serializa as reservas do mesmo quarto. */
    public Optional<Room> lockRoom(Connection c, int number) throws SQLException {
        return room(c, number, " FOR UPDATE");
    }

    public Optional<Room> room(Connection c, int number) throws SQLException {
        return room(c, number, "");
    }

    private Optional<Room> room(Connection c, int number, String suffix) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?" + suffix)) {
            ps.setInt(1, number);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(room(rs)) : Optional.empty();
            }
        }
    }

    private static Room room(ResultSet rs) throws SQLException {
        return new Room(rs.getInt(1), rs.getString(2), rs.getInt(3), Money.round(rs.getBigDecimal(4)));
    }

    // ---- reservas

    public boolean isRoomBusy(Connection c, int room, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM reservations WHERE room_number = ? AND status <> 'CANCELLED' "
                        + "AND check_in < ? AND check_out > ? LIMIT 1")) {
            ps.setInt(1, room);
            ps.setObject(2, checkOut);
            ps.setObject(3, checkIn);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public long insert(Connection c, Reservation r) throws SQLException {
        String cpf = null, cnpj = null, company = null, passport = null, country = null;
        switch (r.identification()) {
            case Identification.Cpf id -> cpf = id.cpf();
            case Identification.Company id -> { cnpj = id.cnpj(); company = id.name(); }
            case Identification.Passport id -> { passport = id.number(); country = id.country(); }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name, "
                        + "guest_passport, guest_country, check_in, check_out, guests, rate_plan, nights, subtotal, "
                        + "discount, total, status, amount_paid) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id")) {
            int i = 1;
            ps.setInt(i++, r.roomNumber());
            ps.setString(i++, r.guestName());
            ps.setString(i++, r.guestEmail());
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
            ps.setBigDecimal(i, r.amountPaid());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public Optional<Reservation> find(Connection c, long id, boolean lock) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM reservations WHERE id = ?" + (lock ? " FOR UPDATE" : ""))) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(reservation(rs)) : Optional.empty();
            }
        }
    }

    public void updateState(Connection c, long id, Status status, BigDecimal amountPaid, BigDecimal refund)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setBigDecimal(2, amountPaid);
            ps.setBigDecimal(3, refund);
            ps.setLong(4, id);
            ps.executeUpdate();
        }
    }

    /** Reservas pagas que ocupam algum quarto na noite indicada, com o tipo do quarto. */
    public record Occupied(int room, String type, long reservation, LocalDate checkIn, LocalDate checkOut) {}

    public List<Occupied> occupancy(Connection c, LocalDate night) throws SQLException {
        List<Occupied> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT r.room_number, m.type, r.id, r.check_in, r.check_out FROM reservations r "
                        + "JOIN rooms m ON m.number = r.room_number "
                        + "WHERE r.status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT') "
                        + "AND r.check_in <= ? AND r.check_out > ? ORDER BY r.room_number, r.id")) {
            ps.setObject(1, night);
            ps.setObject(2, night);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Occupied(rs.getInt(1), rs.getString(2), rs.getLong(3),
                            rs.getObject(4, LocalDate.class), rs.getObject(5, LocalDate.class)));
                }
            }
        }
        return out;
    }

    /** Reservas que começam entre as datas (inclusive) e deixaram dinheiro com a pousada. */
    public List<Reservation> kept(Connection c, LocalDate from, LocalDate to) throws SQLException {
        List<Reservation> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM reservations WHERE check_in BETWEEN ? AND ? "
                        + "AND amount_paid - COALESCE(refund, 0) > 0 ORDER BY check_in, id")) {
            ps.setObject(1, from);
            ps.setObject(2, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(reservation(rs));
            }
        }
        return out;
    }

    private static Reservation reservation(ResultSet rs) throws SQLException {
        Identification ident;
        if (rs.getString("guest_cpf") != null) {
            ident = new Identification.Cpf(rs.getString("guest_cpf"));
        } else if (rs.getString("company_cnpj") != null) {
            ident = new Identification.Company(rs.getString("company_cnpj"), rs.getString("company_name"));
        } else {
            ident = new Identification.Passport(rs.getString("guest_passport"), rs.getString("guest_country"));
        }
        BigDecimal refund = rs.getBigDecimal("refund");
        return new Reservation(rs.getLong("id"), rs.getInt("room_number"), rs.getString("guest_name"),
                rs.getString("guest_email"), ident, rs.getObject("check_in", LocalDate.class),
                rs.getObject("check_out", LocalDate.class), rs.getInt("guests"),
                RatePlan.valueOf(rs.getString("rate_plan")), rs.getInt("nights"),
                Money.round(rs.getBigDecimal("subtotal")), Money.round(rs.getBigDecimal("discount")),
                Money.round(rs.getBigDecimal("total")), Status.valueOf(rs.getString("status")),
                Money.round(rs.getBigDecimal("amount_paid")), refund == null ? null : Money.round(refund));
    }

    // ---- mensagens

    public void addMessage(Connection c, String channel, String to, String subject, String body) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO messages (channel, to_addr, subject, body) VALUES (?,?,?,?)")) {
            ps.setString(1, channel);
            ps.setString(2, to);
            ps.setString(3, subject);
            ps.setString(4, body);
            ps.executeUpdate();
        }
    }

    public List<Message> messages(Connection c) throws SQLException {
        List<Message> out = new ArrayList<>();
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, channel, to_addr, subject, body FROM messages ORDER BY id")) {
            while (rs.next()) {
                out.add(new Message(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)));
            }
        }
        return out;
    }
}
