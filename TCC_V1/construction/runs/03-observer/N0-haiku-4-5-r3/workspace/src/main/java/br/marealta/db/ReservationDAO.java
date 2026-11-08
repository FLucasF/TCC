package br.marealta.db;

import br.marealta.model.RatePlan;
import br.marealta.model.Reservation;
import br.marealta.model.ReservationStatus;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationDAO {
    private final Database db;

    public ReservationDAO(Database db) {
        this.db = db;
    }

    public Reservation save(Reservation reservation) {
        try (Connection conn = db.getConnection()) {
            if (reservation.getId() == 0) {
                return insert(conn, reservation);
            } else {
                update(conn, reservation);
                return reservation;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Reservation insert(Connection conn, Reservation reservation) throws SQLException {
        String sql = "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, " +
                "check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, reservation.getRoomNumber());
            pstmt.setString(2, reservation.getGuestName());
            pstmt.setString(3, reservation.getGuestEmail());
            pstmt.setString(4, reservation.getGuestCpf());
            pstmt.setDate(5, Date.valueOf(reservation.getCheckIn()));
            pstmt.setDate(6, Date.valueOf(reservation.getCheckOut()));
            pstmt.setInt(7, reservation.getGuests());
            pstmt.setString(8, reservation.getRatePlan().name());
            pstmt.setInt(9, reservation.getNights());
            pstmt.setBigDecimal(10, reservation.getSubtotal());
            pstmt.setBigDecimal(11, reservation.getDiscount());
            pstmt.setBigDecimal(12, reservation.getTotal());
            pstmt.setString(13, reservation.getStatus().name());
            pstmt.setBigDecimal(14, reservation.getAmountPaid());

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                reservation.setId(rs.getLong("id"));
            }
        }
        return reservation;
    }

    private void update(Connection conn, Reservation reservation) throws SQLException {
        String sql = "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, reservation.getStatus().name());
            pstmt.setBigDecimal(2, reservation.getAmountPaid());
            pstmt.setBigDecimal(3, reservation.getRefund());
            pstmt.setLong(4, reservation.getId());
            pstmt.executeUpdate();
        }
    }

    public Optional<Reservation> findById(long id) {
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT * FROM reservations WHERE id = " + id);
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    public List<Reservation> findByRoomAndDateRange(int roomNumber, LocalDate start, LocalDate end) {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT * FROM reservations WHERE room_number = ? " +
                "AND check_in < ? AND check_out > ? " +
                "AND status != 'CANCELLED' " +
                "ORDER BY check_in";

        try (Connection conn = db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, roomNumber);
            pstmt.setDate(2, Date.valueOf(end));
            pstmt.setDate(3, Date.valueOf(start));

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return reservations;
    }

    public List<Reservation> findByMonth(int year, int month) {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT * FROM reservations WHERE " +
                "EXTRACT(YEAR FROM check_in) = ? AND EXTRACT(MONTH FROM check_in) = ? " +
                "AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') " +
                "ORDER BY check_in, id";

        try (Connection conn = db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, year);
            pstmt.setInt(2, month);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return reservations;
    }

    public List<Reservation> findByDate(LocalDate date) {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT * FROM reservations WHERE " +
                "check_in <= ? AND check_out > ? " +
                "AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') " +
                "ORDER BY room_number";

        try (Connection conn = db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, Date.valueOf(date));
            pstmt.setDate(2, Date.valueOf(date));

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return reservations;
    }

    private Reservation mapRow(ResultSet rs) throws SQLException {
        Reservation res = new Reservation();
        res.setId(rs.getLong("id"));
        res.setRoomNumber(rs.getInt("room_number"));
        res.setGuestName(rs.getString("guest_name"));
        res.setGuestEmail(rs.getString("guest_email"));
        res.setGuestCpf(rs.getString("guest_cpf"));
        res.setCheckIn(rs.getDate("check_in").toLocalDate());
        res.setCheckOut(rs.getDate("check_out").toLocalDate());
        res.setGuests(rs.getInt("guests"));
        res.setRatePlan(RatePlan.valueOf(rs.getString("rate_plan")));
        res.setNights(rs.getInt("nights"));
        res.setSubtotal(rs.getBigDecimal("subtotal"));
        res.setDiscount(rs.getBigDecimal("discount"));
        res.setTotal(rs.getBigDecimal("total"));
        res.setStatus(ReservationStatus.valueOf(rs.getString("status")));
        res.setAmountPaid(rs.getBigDecimal("amount_paid"));
        res.setRefund(rs.getBigDecimal("refund"));
        return res;
    }
}
