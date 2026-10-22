package pousada.infra;

import pousada.domain.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    private static final String COLUMNS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
            + "guests, rate_plan, subtotal, discount, total, status, amount_paid, refund";

    private final RoomCatalog rooms;

    public ReservationRepository(RoomCatalog rooms) {
        this.rooms = rooms;
    }

    /** Serializa quem mexe nas reservas do mesmo quarto até o fim da transação. */
    public void lockRoom(Connection c, Room room) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
            ps.setLong(1, room.number());
            ps.execute();
        }
    }

    public boolean existsOverlapping(Connection c, Room room, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        String sql = "SELECT 1 FROM reservations WHERE room_number = ? AND status <> 'CANCELLED' "
                + "AND check_in < ? AND check_out > ? LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, room.number());
            ps.setObject(2, checkOut);
            ps.setObject(3, checkIn);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Reservation insert(Connection c, Reservation r) throws SQLException {
        String sql = "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
                + "guests, rate_plan, subtotal, discount, total, status, amount_paid, refund) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, r.room().number());
            ps.setString(2, r.guest().name());
            ps.setString(3, r.guest().email());
            ps.setString(4, r.guest().cpf());
            ps.setObject(5, r.checkIn());
            ps.setObject(6, r.checkOut());
            ps.setInt(7, r.guests());
            ps.setString(8, r.ratePlan().name());
            ps.setBigDecimal(9, r.quote().subtotal());
            ps.setBigDecimal(10, r.quote().discount());
            ps.setBigDecimal(11, r.quote().total());
            ps.setString(12, r.status().name());
            ps.setBigDecimal(13, r.amountPaid());
            ps.setBigDecimal(14, r.refund());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return r.withId(rs.getLong(1));
            }
        }
    }

    public Optional<Reservation> find(Connection c, long id, boolean forUpdate) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM reservations WHERE id = ?" + (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement ps = c.prepareStatement(sql)) {
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

    /** Reservas pagas que ocupam um quarto na noite indicada, por número do quarto. */
    public List<Reservation> findPaidOccupying(Connection c, LocalDate night) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM reservations WHERE status = ANY (?) "
                + "AND check_in <= ? AND check_out > ? ORDER BY room_number, id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setArray(1, c.createArrayOf("varchar",
                    ReservationStatus.paidOnes().stream().map(Enum::name).toArray()));
            ps.setObject(2, night);
            ps.setObject(3, night);
            return readAll(ps);
        }
    }

    /** Reservas com entrada em [from, to), por data de entrada e número. */
    public List<Reservation> findCheckingInBetween(Connection c, LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM reservations WHERE check_in >= ? AND check_in < ? "
                + "ORDER BY check_in, id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, from);
            ps.setObject(2, to);
            return readAll(ps);
        }
    }

    private List<Reservation> readAll(PreparedStatement ps) throws SQLException {
        List<Reservation> result = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(read(rs));
            }
        }
        return result;
    }

    private Reservation read(ResultSet rs) throws SQLException {
        LocalDate checkIn = rs.getObject("check_in", LocalDate.class);
        LocalDate checkOut = rs.getObject("check_out", LocalDate.class);
        Quote quote = new Quote(Quote.nightsBetween(checkIn, checkOut), rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("discount"), rs.getBigDecimal("total"));
        return new Reservation(rs.getLong("id"),
                rooms.find(rs.getInt("room_number")).orElseThrow(),
                new Guest(rs.getString("guest_name"), rs.getString("guest_email"), rs.getString("guest_cpf")),
                checkIn, checkOut, rs.getInt("guests"), RatePlan.valueOf(rs.getString("rate_plan")), quote,
                ReservationStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("amount_paid"), rs.getBigDecimal("refund"));
    }
}
