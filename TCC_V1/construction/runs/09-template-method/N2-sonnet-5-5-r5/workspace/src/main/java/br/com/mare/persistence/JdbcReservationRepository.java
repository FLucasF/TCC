package br.com.mare.persistence;

import br.com.mare.domain.RatePlan;
import br.com.mare.domain.Reservation;
import br.com.mare.domain.ReservationStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

public class JdbcReservationRepository {
    private final DataSource dataSource;

    public JdbcReservationRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void insert(Reservation r) {
        String sql = """
                INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                    guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            ps.setInt(i++, r.roomNumber());
            ps.setString(i++, r.guestName());
            ps.setString(i++, r.guestEmail());
            ps.setString(i++, r.guestCpf());
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
                r.assignId(keys.getLong("id"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public void update(Reservation r) {
        String sql = "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, r.status().name());
            ps.setBigDecimal(2, r.amountPaid());
            ps.setBigDecimal(3, r.refund());
            ps.setLong(4, r.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public Optional<Reservation> findById(long id) {
        List<Reservation> found = query("SELECT * FROM reservations WHERE id = ?", id);
        return found.stream().findFirst();
    }

    public List<Reservation> findAll() {
        return query("SELECT * FROM reservations ORDER BY id");
    }

    /** Reservas não canceladas do quarto. */
    public List<Reservation> findActiveByRoom(int roomNumber) {
        return query("SELECT * FROM reservations WHERE room_number = ? AND status <> 'CANCELLED' ORDER BY id",
                roomNumber);
    }

    private List<Reservation> query(String sql, Object... params) {
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            List<Reservation> result = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private Reservation map(ResultSet rs) throws SQLException {
        BigDecimal refund = rs.getBigDecimal("refund");
        return new Reservation(
                rs.getLong("id"),
                rs.getInt("room_number"),
                rs.getString("guest_name"),
                rs.getString("guest_email"),
                rs.getString("guest_cpf"),
                rs.getObject("check_in", LocalDate.class),
                rs.getObject("check_out", LocalDate.class),
                rs.getInt("guests"),
                RatePlan.valueOf(rs.getString("rate_plan")),
                rs.getInt("nights"),
                rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("discount"),
                rs.getBigDecimal("total"),
                ReservationStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("amount_paid"),
                refund);
    }
}
