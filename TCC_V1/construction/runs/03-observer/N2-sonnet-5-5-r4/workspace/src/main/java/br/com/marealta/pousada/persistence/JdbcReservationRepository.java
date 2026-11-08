package br.com.marealta.pousada.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import br.com.marealta.pousada.domain.*;

public class JdbcReservationRepository implements ReservationRepository {

    private final Database db;
    private final RoomCatalog rooms;

    public JdbcReservationRepository(Database db, RoomCatalog rooms) {
        this.db = db;
        this.rooms = rooms;
    }

    @Override
    public Reservation insert(Reservation r) {
        String sql = """
                INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                    guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""";
        try (Connection c = db.connection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.room().number());
            ps.setString(2, r.guest().name());
            ps.setString(3, r.guest().email());
            ps.setString(4, r.guest().cpf());
            ps.setObject(5, r.checkIn());
            ps.setObject(6, r.checkOut());
            ps.setInt(7, r.guests());
            ps.setString(8, r.ratePlan().name());
            ps.setInt(9, r.quote().nights());
            ps.setBigDecimal(10, r.quote().subtotal());
            ps.setBigDecimal(11, r.quote().discount());
            ps.setBigDecimal(12, r.quote().total());
            ps.setString(13, r.status().name());
            ps.setBigDecimal(14, r.amountPaid());
            ps.setBigDecimal(15, r.refund());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return r.withId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void update(Reservation r) {
        String sql = "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?";
        try (Connection c = db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, r.status().name());
            ps.setBigDecimal(2, r.amountPaid());
            ps.setBigDecimal(3, r.refund());
            ps.setLong(4, r.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Optional<Reservation> find(long id) {
        try (Connection c = db.connection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM reservations WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(read(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<Reservation> findAll() {
        try (Connection c = db.connection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM reservations ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            List<Reservation> result = new ArrayList<>();
            while (rs.next()) {
                result.add(read(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean existsOverlap(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        String sql = """
                SELECT 1 FROM reservations
                WHERE room_number = ? AND status <> 'CANCELLED' AND check_in < ? AND check_out > ?
                LIMIT 1""";
        try (Connection c = db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, roomNumber);
            ps.setObject(2, checkOut);
            ps.setObject(3, checkIn);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private Reservation read(ResultSet rs) throws SQLException {
        Room room = rooms.find(rs.getInt("room_number")).orElseThrow();
        Quote quote = new Quote(rs.getInt("nights"), rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("discount"), rs.getBigDecimal("total"));
        return new Reservation(rs.getLong("id"), room,
                new Guest(rs.getString("guest_name"), rs.getString("guest_email"), rs.getString("guest_cpf")),
                rs.getObject("check_in", LocalDate.class), rs.getObject("check_out", LocalDate.class),
                rs.getInt("guests"), RatePlan.valueOf(rs.getString("rate_plan")), quote,
                ReservationStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("amount_paid"), rs.getBigDecimal("refund"));
    }
}
