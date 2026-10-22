package com.pousada.repositories;

import com.pousada.database.Database;
import com.pousada.models.Reservation;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationRepository {
    private final Database database;

    public ReservationRepository(Database database) {
        this.database = database;
    }

    public Reservation create(Reservation reservation) throws Exception {
        String sql = """
            INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf,
                                     check_in, check_out, guests, rate_plan, nights,
                                     subtotal, discount, total, status, amount_paid)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, reservation.getRoomNumber());
            stmt.setString(2, reservation.getGuestName());
            stmt.setString(3, reservation.getGuestEmail());
            stmt.setString(4, reservation.getGuestCpf());
            stmt.setDate(5, java.sql.Date.valueOf(reservation.getCheckIn()));
            stmt.setDate(6, java.sql.Date.valueOf(reservation.getCheckOut()));
            stmt.setInt(7, reservation.getGuests());
            stmt.setString(8, reservation.getRatePlan());
            stmt.setInt(9, reservation.getNights());
            stmt.setBigDecimal(10, reservation.getSubtotal());
            stmt.setBigDecimal(11, reservation.getDiscount());
            stmt.setBigDecimal(12, reservation.getTotal());
            stmt.setString(13, reservation.getStatus());
            stmt.setBigDecimal(14, reservation.getAmountPaid());

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    reservation.setId(generatedKeys.getLong(1));
                }
            }
        }

        return reservation;
    }

    public Reservation findById(Long id) throws Exception {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                   guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations WHERE id = ?
            """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToReservation(rs);
                }
            }
        }

        return null;
    }

    public void update(Reservation reservation) throws Exception {
        String sql = """
            UPDATE reservations
            SET status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
            """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, reservation.getStatus());
            stmt.setBigDecimal(2, reservation.getAmountPaid());
            stmt.setBigDecimal(3, reservation.getRefund());
            stmt.setLong(4, reservation.getId());

            stmt.executeUpdate();
        }
    }

    public boolean isRoomAvailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws Exception {
        String sql = """
            SELECT COUNT(*) as count FROM reservations
            WHERE room_number = ?
            AND status != 'CANCELLED'
            AND check_in < ? AND check_out > ?
            """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roomNumber);
            stmt.setDate(2, java.sql.Date.valueOf(checkOut));
            stmt.setDate(3, java.sql.Date.valueOf(checkIn));

            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt("count") == 0;
            }
        }
    }

    public List<Reservation> findByRoomAndNight(int roomNumber, LocalDate night) throws Exception {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                   guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE room_number = ?
            AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            AND check_in <= ? AND check_out > ?
            ORDER BY check_in
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roomNumber);
            stmt.setDate(2, java.sql.Date.valueOf(night));
            stmt.setDate(3, java.sql.Date.valueOf(night));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRowToReservation(rs));
                }
            }
        }

        return reservations;
    }

    public List<Reservation> findByMonthAndNotCancelled(int year, int month) throws Exception {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                   guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE EXTRACT(YEAR FROM check_in) = ?
            AND EXTRACT(MONTH FROM check_in) = ?
            AND status != 'CANCELLED'
            ORDER BY check_in, id
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, year);
            stmt.setInt(2, month);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRowToReservation(rs));
                }
            }
        }

        return reservations;
    }

    private Reservation mapRowToReservation(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getLong("id"));
        r.setRoomNumber(rs.getInt("room_number"));
        r.setGuestName(rs.getString("guest_name"));
        r.setGuestEmail(rs.getString("guest_email"));
        r.setGuestCpf(rs.getString("guest_cpf"));
        r.setCheckIn(rs.getDate("check_in").toLocalDate());
        r.setCheckOut(rs.getDate("check_out").toLocalDate());
        r.setGuests(rs.getInt("guests"));
        r.setRatePlan(rs.getString("rate_plan"));
        r.setNights(rs.getInt("nights"));
        r.setSubtotal(rs.getBigDecimal("subtotal"));
        r.setDiscount(rs.getBigDecimal("discount"));
        r.setTotal(rs.getBigDecimal("total"));
        r.setStatus(rs.getString("status"));
        r.setAmountPaid(rs.getBigDecimal("amount_paid"));
        r.setRefund(rs.getBigDecimal("refund"));
        return r;
    }
}
