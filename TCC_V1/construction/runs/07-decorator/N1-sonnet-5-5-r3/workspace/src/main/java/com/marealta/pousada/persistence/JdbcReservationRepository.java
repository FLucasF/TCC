package com.marealta.pousada.persistence;

import com.marealta.pousada.domain.*;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcReservationRepository implements ReservationRepository {
    private static final String COLUMNS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
            + "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund";

    private final DataSource dataSource;

    public JdbcReservationRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public long insert(Reservation r) {
        String sql = "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
                + "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = dataSource.getConnection();
             PreparedStatement s = c.prepareStatement(sql, new String[]{"id"})) {
            s.setInt(1, r.roomNumber());
            s.setString(2, r.guestName());
            s.setString(3, r.guestEmail());
            s.setString(4, r.guestCpf());
            s.setObject(5, r.checkIn());
            s.setObject(6, r.checkOut());
            s.setInt(7, r.guests());
            s.setString(8, r.ratePlan().name());
            s.setInt(9, r.quote().nights());
            s.setBigDecimal(10, r.quote().subtotal());
            s.setBigDecimal(11, r.quote().discount());
            s.setBigDecimal(12, r.quote().total());
            s.setString(13, r.status().name());
            s.setBigDecimal(14, r.amountPaid());
            s.setBigDecimal(15, r.refund());
            s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void update(Reservation r) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement s = c.prepareStatement(
                     "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
            s.setString(1, r.status().name());
            s.setBigDecimal(2, r.amountPaid());
            s.setBigDecimal(3, r.refund());
            s.setLong(4, r.id());
            s.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Optional<Reservation> find(long id) {
        return query("WHERE id = ?", s -> s.setLong(1, id)).stream().findFirst();
    }

    @Override
    public List<Reservation> findActiveOverlapping(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        return query("WHERE room_number = ? AND status <> 'CANCELLED' AND check_in < ? AND check_out > ?", s -> {
            s.setInt(1, roomNumber);
            s.setObject(2, checkOut);
            s.setObject(3, checkIn);
        });
    }

    @Override
    public List<Reservation> findOverlapping(LocalDate checkIn, LocalDate checkOut) {
        return query("WHERE check_in < ? AND check_out > ?", s -> {
            s.setObject(1, checkOut);
            s.setObject(2, checkIn);
        });
    }

    @Override
    public List<Reservation> findByCheckInBetween(LocalDate from, LocalDate to) {
        return query("WHERE check_in >= ? AND check_in <= ?", s -> {
            s.setObject(1, from);
            s.setObject(2, to);
        });
    }

    private interface Binder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    private List<Reservation> query(String where, Binder binder) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement s = c.prepareStatement("SELECT " + COLUMNS + " FROM reservations " + where + " ORDER BY id")) {
            binder.bind(s);
            try (ResultSet rs = s.executeQuery()) {
                List<Reservation> found = new ArrayList<>();
                while (rs.next()) {
                    found.add(read(rs));
                }
                return found;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private Reservation read(ResultSet rs) throws SQLException {
        Quote quote = new Quote(rs.getInt("nights"), rs.getBigDecimal("subtotal"), rs.getBigDecimal("discount"),
                rs.getBigDecimal("total"));
        return new Reservation(rs.getLong("id"), rs.getInt("room_number"), rs.getString("guest_name"),
                rs.getString("guest_email"), rs.getString("guest_cpf"), rs.getObject("check_in", LocalDate.class),
                rs.getObject("check_out", LocalDate.class), rs.getInt("guests"),
                RatePlan.valueOf(rs.getString("rate_plan")), quote, ReservationStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("amount_paid"), rs.getBigDecimal("refund"));
    }
}
