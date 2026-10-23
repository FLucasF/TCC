package com.pousadamarealta.database;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationDAO {
    private final DatabaseManager dbManager;

    public ReservationDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public Reservation save(Reservation reservation) {
        String sql = """
            INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                                      guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = dbManager.getConnection();
             var pstmt = conn.prepareStatement(sql, new String[]{"id"})) {

            pstmt.setString(1, reservation.getRoomNumber());
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
            pstmt.setBigDecimal(15, reservation.getRefund());

            pstmt.executeUpdate();

            try (var rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    reservation.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save reservation", e);
        }

        return reservation;
    }

    public void update(Reservation reservation) {
        String sql = """
            UPDATE reservations
            SET status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
            """;

        try (Connection conn = dbManager.getConnection();
             var pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, reservation.getStatus().name());
            pstmt.setBigDecimal(2, reservation.getAmountPaid());
            pstmt.setBigDecimal(3, reservation.getRefund());
            pstmt.setLong(4, reservation.getId());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation", e);
        }
    }

    public Optional<Reservation> findById(Long id) {
        String sql = "SELECT * FROM reservations WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             var pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            try (var rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Reservation reservation = new Reservation();
                    reservation.setId(rs.getLong("id"));
                    reservation.setRoomNumber(rs.getString("room_number"));
                    reservation.setGuestName(rs.getString("guest_name"));
                    reservation.setGuestEmail(rs.getString("guest_email"));
                    reservation.setGuestCpf(rs.getString("guest_cpf"));
                    reservation.setCheckIn(rs.getDate("check_in").toLocalDate());
                    reservation.setCheckOut(rs.getDate("check_out").toLocalDate());
                    reservation.setGuests(rs.getInt("guests"));
                    reservation.setRatePlan(RatePlan.valueOf(rs.getString("rate_plan")));
                    reservation.setNights(rs.getInt("nights"));
                    reservation.setSubtotal(rs.getBigDecimal("subtotal"));
                    reservation.setDiscount(rs.getBigDecimal("discount"));
                    reservation.setTotal(rs.getBigDecimal("total"));
                    reservation.setStatus(ReservationStatus.valueOf(rs.getString("status")));
                    reservation.setAmountPaid(rs.getBigDecimal("amount_paid"));
                    var refund = rs.getBigDecimal("refund");
                    reservation.setRefund(refund);
                    return Optional.of(reservation);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservation", e);
        }

        return Optional.empty();
    }

    public boolean hasConflictingReservation(String roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeId) {
        String sql = """
            SELECT COUNT(*) FROM reservations
            WHERE room_number = ?
            AND status != 'CANCELLED'
            AND check_in < ?
            AND check_out > ?
            AND (? IS NULL OR id != ?)
            """;

        try (Connection conn = dbManager.getConnection();
             var pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, roomNumber);
            pstmt.setDate(2, Date.valueOf(checkOut));
            pstmt.setDate(3, Date.valueOf(checkIn));
            pstmt.setObject(4, excludeId);
            pstmt.setObject(5, excludeId);

            try (var rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check reservation conflicts", e);
        }

        return false;
    }

    public List<Reservation> findByRoomAndDate(String roomNumber, LocalDate date) {
        String sql = """
            SELECT * FROM reservations
            WHERE room_number = ?
            AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            AND check_in <= ?
            AND check_out > ?
            ORDER BY check_in
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             var pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, roomNumber);
            pstmt.setDate(2, Date.valueOf(date));
            pstmt.setDate(3, Date.valueOf(date));

            try (var rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Reservation reservation = new Reservation();
                    reservation.setId(rs.getLong("id"));
                    reservation.setRoomNumber(rs.getString("room_number"));
                    reservation.setGuestName(rs.getString("guest_name"));
                    reservation.setGuestEmail(rs.getString("guest_email"));
                    reservation.setGuestCpf(rs.getString("guest_cpf"));
                    reservation.setCheckIn(rs.getDate("check_in").toLocalDate());
                    reservation.setCheckOut(rs.getDate("check_out").toLocalDate());
                    reservation.setGuests(rs.getInt("guests"));
                    reservation.setRatePlan(RatePlan.valueOf(rs.getString("rate_plan")));
                    reservation.setNights(rs.getInt("nights"));
                    reservation.setSubtotal(rs.getBigDecimal("subtotal"));
                    reservation.setDiscount(rs.getBigDecimal("discount"));
                    reservation.setTotal(rs.getBigDecimal("total"));
                    reservation.setStatus(ReservationStatus.valueOf(rs.getString("status")));
                    reservation.setAmountPaid(rs.getBigDecimal("amount_paid"));
                    reservation.setRefund(rs.getBigDecimal("refund"));
                    reservations.add(reservation);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservations by room and date", e);
        }

        return reservations;
    }

    public List<Reservation> findByCheckInMonth(int year, int month) {
        String sql = """
            SELECT * FROM reservations
            WHERE EXTRACT(YEAR FROM check_in) = ?
            AND EXTRACT(MONTH FROM check_in) = ?
            ORDER BY check_in, id
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             var pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, year);
            pstmt.setInt(2, month);

            try (var rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Reservation reservation = new Reservation();
                    reservation.setId(rs.getLong("id"));
                    reservation.setRoomNumber(rs.getString("room_number"));
                    reservation.setGuestName(rs.getString("guest_name"));
                    reservation.setGuestEmail(rs.getString("guest_email"));
                    reservation.setGuestCpf(rs.getString("guest_cpf"));
                    reservation.setCheckIn(rs.getDate("check_in").toLocalDate());
                    reservation.setCheckOut(rs.getDate("check_out").toLocalDate());
                    reservation.setGuests(rs.getInt("guests"));
                    reservation.setRatePlan(RatePlan.valueOf(rs.getString("rate_plan")));
                    reservation.setNights(rs.getInt("nights"));
                    reservation.setSubtotal(rs.getBigDecimal("subtotal"));
                    reservation.setDiscount(rs.getBigDecimal("discount"));
                    reservation.setTotal(rs.getBigDecimal("total"));
                    reservation.setStatus(ReservationStatus.valueOf(rs.getString("status")));
                    reservation.setAmountPaid(rs.getBigDecimal("amount_paid"));
                    reservation.setRefund(rs.getBigDecimal("refund"));
                    reservations.add(reservation);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservations by check-in month", e);
        }

        return reservations;
    }
}
