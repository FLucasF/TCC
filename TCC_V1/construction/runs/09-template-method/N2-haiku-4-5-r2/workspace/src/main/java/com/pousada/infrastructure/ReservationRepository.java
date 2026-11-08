package com.pousada.infrastructure;

import com.pousada.domain.RatePlan;
import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    public Reservation save(Reservation reservation) throws Exception {
        String sql = """
            INSERT INTO reservations (
                room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL)
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, reservation.getRoomNumber());
            stmt.setString(2, reservation.getGuestName());
            stmt.setString(3, reservation.getGuestEmail());
            stmt.setString(4, reservation.getGuestCpf());
            stmt.setDate(5, Date.valueOf(reservation.getCheckIn()));
            stmt.setDate(6, Date.valueOf(reservation.getCheckOut()));
            stmt.setInt(7, reservation.getGuests());
            stmt.setString(8, reservation.getRatePlan().getDisplayName());
            stmt.setInt(9, reservation.getNights());
            stmt.setDouble(10, reservation.getSubtotal());
            stmt.setDouble(11, reservation.getDiscount());
            stmt.setDouble(12, reservation.getTotal());
            stmt.setString(13, reservation.getStatus().name());
            stmt.setDouble(14, reservation.getAmountPaid());

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    reservation.setId(rs.getLong(1));
                }
            }
        }
        return reservation;
    }

    public Optional<Reservation> findById(long id) throws Exception {
        String sql = "SELECT * FROM reservations WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToReservation(rs));
                }
            }
        }
        return Optional.empty();
    }

    public void update(Reservation reservation) throws Exception {
        String sql = """
            UPDATE reservations
            SET status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, reservation.getStatus().name());
            stmt.setDouble(2, reservation.getAmountPaid());
            stmt.setObject(3, reservation.getRefund());
            stmt.setLong(4, reservation.getId());

            stmt.executeUpdate();
        }
    }

    public List<Reservation> findByRoomAndNight(int roomNumber, LocalDate date) throws Exception {
        List<Reservation> reservations = new ArrayList<>();
        String sql = """
            SELECT * FROM reservations
            WHERE room_number = ? AND check_in <= ? AND check_out > ?
            AND status != 'CANCELLED'
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roomNumber);
            stmt.setDate(2, Date.valueOf(date));
            stmt.setDate(3, Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapResultSetToReservation(rs));
                }
            }
        }
        return reservations;
    }

    public List<Reservation> findByMonthStarting(LocalDate startOfMonth) throws Exception {
        List<Reservation> reservations = new ArrayList<>();
        String sql = """
            SELECT * FROM reservations
            WHERE EXTRACT(YEAR FROM check_in) = ?
            AND EXTRACT(MONTH FROM check_in) = ?
            ORDER BY check_in, id
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, startOfMonth.getYear());
            stmt.setInt(2, startOfMonth.getMonthValue());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapResultSetToReservation(rs));
                }
            }
        }
        return reservations;
    }

    private Reservation mapResultSetToReservation(ResultSet rs) throws Exception {
        Reservation reservation = new Reservation();
        reservation.setId(rs.getLong("id"));
        reservation.setRoomNumber(rs.getInt("room_number"));
        reservation.setGuestName(rs.getString("guest_name"));
        reservation.setGuestEmail(rs.getString("guest_email"));
        reservation.setGuestCpf(rs.getString("guest_cpf"));
        reservation.setCheckIn(rs.getDate("check_in").toLocalDate());
        reservation.setCheckOut(rs.getDate("check_out").toLocalDate());
        reservation.setGuests(rs.getInt("guests"));
        reservation.setRatePlan(RatePlan.valueOf(rs.getString("rate_plan")));
        reservation.setNights(rs.getInt("nights"));
        reservation.setSubtotal(rs.getDouble("subtotal"));
        reservation.setDiscount(rs.getDouble("discount"));
        reservation.setTotal(rs.getDouble("total"));
        reservation.setStatus(ReservationStatus.valueOf(rs.getString("status")));
        reservation.setAmountPaid(rs.getDouble("amount_paid"));
        Double refund = rs.getDouble("refund");
        if (!rs.wasNull()) {
            reservation.setRefund(refund);
        }
        return reservation;
    }
}
