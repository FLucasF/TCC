package br.com.marealta.pousada;

import br.com.marealta.pousada.Domain.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Acesso ao banco dentro de uma transação. */
public class Repo implements MailDispatcher.MailStore {
    private final Connection c;

    Repo(Connection c) {
        this.c = c;
    }

    private static RuntimeException wrap(SQLException e) {
        return new IllegalStateException(e);
    }

    void createSchema() {
        try (Statement st = c.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS rooms (
                      number INT PRIMARY KEY,
                      type TEXT NOT NULL,
                      capacity INT NOT NULL,
                      nightly_rate NUMERIC(12,2) NOT NULL)""");
            st.execute("""
                    CREATE TABLE IF NOT EXISTS reservations (
                      id BIGSERIAL PRIMARY KEY,
                      room_number INT NOT NULL REFERENCES rooms(number),
                      guest_name TEXT NOT NULL,
                      guest_email TEXT NOT NULL,
                      guest_cpf TEXT NOT NULL,
                      check_in DATE NOT NULL,
                      check_out DATE NOT NULL,
                      guests INT NOT NULL,
                      rate_plan TEXT NOT NULL,
                      nights INT NOT NULL,
                      subtotal NUMERIC(12,2) NOT NULL,
                      discount NUMERIC(12,2) NOT NULL,
                      total NUMERIC(12,2) NOT NULL,
                      status TEXT NOT NULL,
                      amount_paid NUMERIC(12,2) NOT NULL,
                      refund NUMERIC(12,2))""");
            st.execute("""
                    CREATE TABLE IF NOT EXISTS messages (
                      id BIGSERIAL PRIMARY KEY,
                      channel TEXT NOT NULL,
                      to_addr TEXT NOT NULL,
                      subject TEXT NOT NULL,
                      body TEXT NOT NULL,
                      status TEXT NOT NULL,
                      original_to TEXT NOT NULL,
                      copy BOOLEAN NOT NULL,
                      created_at TIMESTAMPTZ NOT NULL)""");
            st.execute("""
                    INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES
                      (101, 'STANDARD', 2, 249.90), (102, 'STANDARD', 2, 249.90),
                      (201, 'DELUXE', 3, 389.90), (202, 'DELUXE', 3, 389.90),
                      (301, 'SUITE', 4, 579.90)
                    ON CONFLICT (number) DO NOTHING""");
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    // ---- quartos

    private static Room room(ResultSet rs) throws SQLException {
        return new Room(rs.getInt("number"), RoomType.valueOf(rs.getString("type")),
                rs.getInt("capacity"), rs.getBigDecimal("nightly_rate"));
    }

    public List<Room> rooms() {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM rooms ORDER BY number")) {
            List<Room> out = new ArrayList<>();
            while (rs.next()) out.add(room(rs));
            return out;
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    public Optional<Room> room(int number) {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM rooms WHERE number = ?")) {
            ps.setInt(1, number);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(room(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    // ---- reservas

    private static Reservation reservation(ResultSet rs) throws SQLException {
        return new Reservation(rs.getLong("id"), rs.getInt("room_number"), rs.getString("guest_name"),
                rs.getString("guest_email"), rs.getString("guest_cpf"),
                rs.getObject("check_in", LocalDate.class), rs.getObject("check_out", LocalDate.class),
                rs.getInt("guests"), RatePlan.valueOf(rs.getString("rate_plan")), rs.getInt("nights"),
                rs.getBigDecimal("subtotal"), rs.getBigDecimal("discount"), rs.getBigDecimal("total"),
                Status.valueOf(rs.getString("status")), rs.getBigDecimal("amount_paid"), rs.getBigDecimal("refund"));
    }

    public Optional<Reservation> reservation(long id) {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM reservations WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(reservation(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    public boolean hasOverlap(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT 1 FROM reservations
                WHERE room_number = ? AND status <> 'CANCELLED' AND check_in < ? AND check_out > ?
                LIMIT 1""")) {
            ps.setInt(1, roomNumber);
            ps.setObject(2, checkOut);
            ps.setObject(3, checkIn);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    public Reservation insert(Reservation r) {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                  guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id""")) {
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
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return reservation(rs.getLong(1)).orElseThrow();
            }
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    public Reservation updateState(long id, Status status, BigDecimal amountPaid, BigDecimal refund) {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setBigDecimal(2, amountPaid);
            ps.setBigDecimal(3, refund);
            ps.setLong(4, id);
            ps.executeUpdate();
            return reservation(id).orElseThrow();
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    /** Reservas pagas que ocupam um quarto na noite indicada, por número do quarto. */
    public List<Reservation> occupying(LocalDate night) {
        return query("""
                SELECT * FROM reservations
                WHERE status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT') AND check_in <= ? AND check_out > ?
                ORDER BY room_number, id""", night, night);
    }

    /** Reservas que começam no período e ficaram com dinheiro (pago menos devolvido). */
    public List<Reservation> revenueStartingBetween(LocalDate fromInclusive, LocalDate toExclusive) {
        return query("""
                SELECT * FROM reservations
                WHERE check_in >= ? AND check_in < ? AND amount_paid - COALESCE(refund, 0) > 0
                ORDER BY check_in, id""", fromInclusive, toExclusive);
    }

    private List<Reservation> query(String sql, Object... params) {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                List<Reservation> out = new ArrayList<>();
                while (rs.next()) out.add(reservation(rs));
                return out;
            }
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    // ---- mensagens

    @Override
    public void save(MailDispatcher.NewMessage m) {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO messages (channel, to_addr, subject, body, status, original_to, copy, created_at)
                VALUES ('EMAIL',?,?,?,?,?,?,?)""")) {
            ps.setString(1, m.to());
            ps.setString(2, m.subject());
            ps.setString(3, m.body());
            ps.setString(4, m.status());
            ps.setString(5, m.originalTo());
            ps.setBoolean(6, m.copy());
            ps.setObject(7, m.createdAt().atOffset(java.time.ZoneOffset.UTC));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    @Override
    public int countSent(String address, Instant from, Instant to) {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT count(*) FROM messages
                WHERE status = 'SENT' AND NOT copy AND lower(original_to) = ?
                  AND created_at >= ? AND created_at < ?""")) {
            ps.setString(1, address);
            ps.setObject(2, from.atOffset(java.time.ZoneOffset.UTC));
            ps.setObject(3, to.atOffset(java.time.ZoneOffset.UTC));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    public List<Message> messages() {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM messages ORDER BY id")) {
            List<Message> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new Message(rs.getLong("id"), rs.getString("channel"), rs.getString("to_addr"),
                        rs.getString("subject"), rs.getString("body"), rs.getString("status")));
            }
            return out;
        } catch (SQLException e) {
            throw wrap(e);
        }
    }
}
