package com.pousadamarealta.repository;

import com.pousadamarealta.database.Database;
import com.pousadamarealta.models.Reservation;
import com.pousadamarealta.models.ReservationStatus;
import com.pousadamarealta.models.RatePlan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    public Reservation create(Reservation res) throws Exception {
        String sql = "INSERT INTO reservations " +
            "(room_number, room_type, guest_name, guest_email, guest_cpf, check_in, check_out, guests, " +
            "rate_plan, nights, subtotal, discount, total, status, amount_paid) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
            "RETURNING id";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, res.roomNumber);
            ps.setString(2, res.roomType);
            ps.setString(3, res.guestName);
            ps.setString(4, res.guestEmail);
            ps.setString(5, res.guestCpf);
            ps.setObject(6, res.checkIn);
            ps.setObject(7, res.checkOut);
            ps.setInt(8, res.guests);
            ps.setString(9, res.ratePlan.name());
            ps.setInt(10, res.nights);
            ps.setBigDecimal(11, res.subtotal);
            ps.setBigDecimal(12, res.discount);
            ps.setBigDecimal(13, res.total);
            ps.setString(14, res.status.name());
            ps.setBigDecimal(15, res.amountPaid);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    res.id = rs.getLong("id");
                    return res;
                }
            }
        }
        throw new Exception("Failed to create reservation");
    }

    public Optional<Reservation> findById(long id) throws Exception {
        String sql = "SELECT * FROM reservations WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapToReservation(rs));
                }
            }
        }
        return Optional.empty();
    }

    public void update(Reservation res) throws Exception {
        String sql = "UPDATE reservations SET " +
            "status = ?, amount_paid = ?, refund = ? WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, res.status.name());
            ps.setBigDecimal(2, res.amountPaid);
            ps.setBigDecimal(3, res.refund);
            ps.setLong(4, res.id);
            ps.executeUpdate();
        }
    }

    public List<Reservation> findByRoomAndDates(
        int roomNumber,
        LocalDate checkIn,
        LocalDate checkOut
    ) throws Exception {
        String sql = "SELECT * FROM reservations " +
            "WHERE room_number = ? " +
            "AND status != 'CANCELLED' " +
            "AND check_in < ? " +
            "AND check_out > ?";

        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomNumber);
            ps.setObject(2, checkOut);
            ps.setObject(3, checkIn);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapToReservation(rs));
                }
            }
        }
        return reservations;
    }

    public List<Reservation> findOccupancyByDate(LocalDate date) throws Exception {
        String sql = "SELECT * FROM reservations " +
            "WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') " +
            "AND check_in <= ? " +
            "AND check_out > ? " +
            "ORDER BY room_number";

        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, date);
            ps.setObject(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapToReservation(rs));
                }
            }
        }
        return reservations;
    }

    public List<Reservation> findRevenueByMonth(int year, int month) throws Exception {
        String sql = "SELECT * FROM reservations " +
            "WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED') " +
            "AND EXTRACT(YEAR FROM check_in) = ? " +
            "AND EXTRACT(MONTH FROM check_in) = ? " +
            "AND (amount_paid > 0 OR refund > 0) " +
            "ORDER BY check_in, id";

        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, year);
            ps.setInt(2, month);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapToReservation(rs));
                }
            }
        }
        return reservations;
    }

    private Reservation mapToReservation(ResultSet rs) throws Exception {
        return new Reservation(
            rs.getLong("id"),
            rs.getInt("room_number"),
            rs.getString("room_type"),
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
            rs.getBigDecimal("refund")
        );
    }
}
