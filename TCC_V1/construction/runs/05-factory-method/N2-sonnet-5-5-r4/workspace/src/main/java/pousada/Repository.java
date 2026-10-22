package pousada;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Acesso às tabelas. Todas as operações rodam na transação recebida. */
class Repository {
    private static final String RESERVATION_SELECT = """
            select r.*, ro.type, ro.capacity, ro.nightly_rate
            from reservations r join rooms ro on ro.number = r.room_number """;

    private final ObjectMapper mapper = new ObjectMapper();

    Optional<Room> findRoomLocked(Connection c, int number) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "select number, type, capacity, nightly_rate from rooms where number = ? for update")) {
            ps.setInt(1, number);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? Optional.of(room(rs, "number")) : Optional.empty();
        }
    }

    List<Room> rooms(Connection c) throws SQLException {
        List<Room> rooms = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "select number, type, capacity, nightly_rate from rooms order by number")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rooms.add(room(rs, "number"));
            }
        }
        return rooms;
    }

    boolean isRoomBooked(Connection c, int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                select 1 from reservations
                where room_number = ? and status <> 'CANCELLED' and check_in < ? and check_out > ?""")) {
            ps.setInt(1, roomNumber);
            ps.setDate(2, Date.valueOf(checkOut));
            ps.setDate(3, Date.valueOf(checkIn));
            return ps.executeQuery().next();
        }
    }

    Reservation insert(Connection c, Reservation r) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                insert into reservations (room_number, guest_name, guest_email, id_type, id_values, check_in,
                  check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund)
                values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) returning id""")) {
            ps.setInt(1, r.room().number());
            ps.setString(2, r.guestName());
            ps.setString(3, r.guestEmail());
            ps.setString(4, r.identification().type().code());
            ps.setString(5, json(r.identification().values()));
            ps.setDate(6, Date.valueOf(r.checkIn()));
            ps.setDate(7, Date.valueOf(r.checkOut()));
            ps.setInt(8, r.guests());
            ps.setString(9, r.ratePlan().name());
            ps.setInt(10, r.pricing().nights());
            ps.setBigDecimal(11, r.pricing().subtotal());
            ps.setBigDecimal(12, r.pricing().discount());
            ps.setBigDecimal(13, r.pricing().total());
            ps.setString(14, r.status().name());
            ps.setBigDecimal(15, r.amountPaid());
            ps.setBigDecimal(16, r.refund());
            ResultSet rs = ps.executeQuery();
            rs.next();
            return new Reservation(rs.getLong(1), r.room(), r.guestName(), r.guestEmail(), r.identification(),
                    r.checkIn(), r.checkOut(), r.guests(), r.ratePlan(), r.pricing(), r.status(),
                    r.amountPaid(), r.refund());
        }
    }

    Optional<Reservation> findLocked(Connection c, long id) throws SQLException {
        return find(c, id, " for update of r");
    }

    Optional<Reservation> find(Connection c, long id) throws SQLException {
        return find(c, id, "");
    }

    void saveState(Connection c, Reservation r) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "update reservations set status = ?, amount_paid = ?, refund = ? where id = ?")) {
            ps.setString(1, r.status().name());
            ps.setBigDecimal(2, r.amountPaid());
            ps.setBigDecimal(3, r.refund());
            ps.setLong(4, r.id());
            ps.executeUpdate();
        }
    }

    List<Reservation> paidOccupying(Connection c, LocalDate night) throws SQLException {
        return query(c, "where r.status in ('CONFIRMED','CHECKED_IN','CHECKED_OUT') "
                + "and r.check_in <= ? and r.check_out > ? order by r.room_number", night, night);
    }

    List<Reservation> startingBetween(Connection c, LocalDate from, LocalDate to) throws SQLException {
        return query(c, "where r.check_in >= ? and r.check_in <= ? and r.amount_paid - coalesce(r.refund, 0) > 0 "
                + "order by r.check_in, r.id", from, to);
    }

    Message saveMessage(Connection c, String channel, String to, String subject, String body) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "insert into messages (channel, recipient, subject, body) values (?,?,?,?) returning id")) {
            ps.setString(1, channel);
            ps.setString(2, to);
            ps.setString(3, subject);
            ps.setString(4, body);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return new Message(rs.getLong(1), channel, to, subject, body);
        }
    }

    List<Message> messages(Connection c) throws SQLException {
        List<Message> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "select id, channel, recipient, subject, body from messages order by id")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Message(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5)));
            }
        }
        return list;
    }

    private Optional<Reservation> find(Connection c, long id, String lock) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(RESERVATION_SELECT + " where r.id = ?" + lock)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? Optional.of(reservation(rs)) : Optional.empty();
        }
    }

    private List<Reservation> query(Connection c, String clause, LocalDate a, LocalDate b) throws SQLException {
        List<Reservation> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(RESERVATION_SELECT + " " + clause)) {
            ps.setDate(1, Date.valueOf(a));
            ps.setDate(2, Date.valueOf(b));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(reservation(rs));
            }
        }
        return list;
    }

    private Reservation reservation(ResultSet rs) throws SQLException {
        Room room = new Room(rs.getInt("room_number"), rs.getString("type"), rs.getInt("capacity"),
                rs.getBigDecimal("nightly_rate"));
        Identification identification = new Identification(IdentificationTypes.byCode(rs.getString("id_type")),
                values(rs.getString("id_values")));
        Pricing pricing = new Pricing(rs.getInt("nights"), rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("discount"), rs.getBigDecimal("total"));
        BigDecimal refund = rs.getBigDecimal("refund");
        return new Reservation(rs.getLong("id"), room, rs.getString("guest_name"), rs.getString("guest_email"),
                identification, rs.getDate("check_in").toLocalDate(), rs.getDate("check_out").toLocalDate(),
                rs.getInt("guests"), RatePlan.valueOf(rs.getString("rate_plan")), pricing,
                Status.valueOf(rs.getString("status")), rs.getBigDecimal("amount_paid"), refund);
    }

    private Room room(ResultSet rs, String numberColumn) throws SQLException {
        return new Room(rs.getInt(numberColumn), rs.getString("type"), rs.getInt("capacity"),
                rs.getBigDecimal("nightly_rate"));
    }

    private String json(Map<String, String> values) {
        try {
            return mapper.writeValueAsString(values);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Map<String, String> values(String json) {
        try {
            return mapper.readValue(json, new TypeReference<>() { });
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
