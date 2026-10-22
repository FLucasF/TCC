package com.pousada.repository;

import com.pousada.Database;
import com.pousada.model.Reservation;
import com.pousada.model.ReservationStatus;
import com.pousada.model.RatePlan;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {

    public static Reservation create(Reservation reservation) {
        String sql = """
                INSERT INTO reservations
                (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, reservation.getRoomNumber());
            pstmt.setString(2, reservation.getGuestName());
            pstmt.setString(3, reservation.getGuestEmail());
            pstmt.setString(4, reservation.getGuestCpf());
            pstmt.setDate(5, Date.valueOf(reservation.getCheckIn()));
            pstmt.setDate(6, Date.valueOf(reservation.getCheckOut()));
            pstmt.setInt(7, reservation.getGuests());
            pstmt.setString(8, reservation.getRatePlan().toString());
            pstmt.setInt(9, reservation.getNights());
            pstmt.setBigDecimal(10, reservation.getSubtotal());
            pstmt.setBigDecimal(11, reservation.getDiscount());
            pstmt.setBigDecimal(12, reservation.getTotal());
            pstmt.setString(13, ReservationStatus.PENDING.toString());
            pstmt.setBigDecimal(14, reservation.getAmountPaid());

            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    reservation.setId(generatedKeys.getInt(1));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return reservation;
    }

    public static Optional<Reservation> findById(int id) {
        String sql = "SELECT * FROM reservations WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapReservation(rs));
            }
            return Optional.empty();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void update(Reservation reservation) {
        String sql = """
                UPDATE reservations
                SET status = ?, amount_paid = ?, refund = ?
                WHERE id = ?
                """;

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, reservation.getStatus().toString());
            pstmt.setBigDecimal(2, reservation.getAmountPaid());
            pstmt.setBigDecimal(3, reservation.getRefund());
            pstmt.setInt(4, reservation.getId());

            pstmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isRoomAvailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        String sql = """
                SELECT COUNT(*) as cnt FROM reservations
                WHERE room_number = ?
                AND status != 'CANCELLED'
                AND check_in < ?
                AND check_out > ?
                """;

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, roomNumber);
            pstmt.setDate(2, Date.valueOf(checkOut));
            pstmt.setDate(3, Date.valueOf(checkIn));

            ResultSet rs = pstmt.executeQuery();
            rs.next();
            return rs.getInt("cnt") == 0;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Reservation> findByMonthStart(int year, int month) {
        List<Reservation> reservations = new ArrayList<>();
        String sql = """
                SELECT * FROM reservations
                WHERE EXTRACT(YEAR FROM check_in) = ? AND EXTRACT(MONTH FROM check_in) = ?
                ORDER BY check_in, id
                """;

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, year);
            pstmt.setInt(2, month);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapReservation(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return reservations;
    }

    public static List<Reservation> findOccupancyByDate(LocalDate date) {
        List<Reservation> reservations = new ArrayList<>();
        String sql = """
                SELECT * FROM reservations
                WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
                AND check_in <= ?
                AND check_out > ?
                ORDER BY room_number
                """;

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, Date.valueOf(date));
            pstmt.setDate(2, Date.valueOf(date));

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapReservation(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return reservations;
    }

    private static Reservation mapReservation(ResultSet rs) throws Exception {
        Reservation reservation = new Reservation();
        reservation.setId(rs.getInt("id"));
        reservation.setRoomNumber(rs.getInt("room_number"));
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

        return reservation;
    }
}
