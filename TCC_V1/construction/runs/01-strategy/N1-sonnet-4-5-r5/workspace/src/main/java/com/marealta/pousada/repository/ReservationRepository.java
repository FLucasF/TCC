package com.marealta.pousada.repository;

import com.marealta.pousada.domain.model.Reservation;
import com.marealta.pousada.domain.model.ReservationStatus;
import com.marealta.pousada.domain.model.RoomType;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {

    private final Connection connection;

    public ReservationRepository(Connection connection) {
        this.connection = connection;
    }

    public void createTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS reservations (
                id BIGSERIAL PRIMARY KEY,
                room_number INTEGER NOT NULL,
                room_type VARCHAR(20) NOT NULL,
                guest_name VARCHAR(255) NOT NULL,
                guest_email VARCHAR(255) NOT NULL,
                guest_cpf VARCHAR(14) NOT NULL,
                check_in DATE NOT NULL,
                check_out DATE NOT NULL,
                guests INTEGER NOT NULL,
                rate_plan VARCHAR(50) NOT NULL,
                nights BIGINT NOT NULL,
                subtotal NUMERIC(10, 2) NOT NULL,
                discount NUMERIC(10, 2) NOT NULL,
                total NUMERIC(10, 2) NOT NULL,
                status VARCHAR(20) NOT NULL,
                amount_paid NUMERIC(10, 2) NOT NULL,
                refund NUMERIC(10, 2)
            )
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.execute();
        }
    }

    public Reservation save(Reservation reservation) throws SQLException {
        String sql = """
            INSERT INTO reservations (room_number, room_type, guest_name, guest_email, guest_cpf,
                                     check_in, check_out, guests, rate_plan, nights,
                                     subtotal, discount, total, status, amount_paid, refund)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, reservation.getRoomNumber());
            stmt.setString(2, reservation.getRoomType().name());
            stmt.setString(3, reservation.getGuestName());
            stmt.setString(4, reservation.getGuestEmail());
            stmt.setString(5, reservation.getGuestCpf());
            stmt.setDate(6, Date.valueOf(reservation.getCheckIn()));
            stmt.setDate(7, Date.valueOf(reservation.getCheckOut()));
            stmt.setInt(8, reservation.getGuests());
            stmt.setString(9, reservation.getRatePlanName());
            stmt.setLong(10, reservation.getNights());
            stmt.setBigDecimal(11, reservation.getSubtotal());
            stmt.setBigDecimal(12, reservation.getDiscount());
            stmt.setBigDecimal(13, reservation.getTotal());
            stmt.setString(14, reservation.getStatus().name());
            stmt.setBigDecimal(15, reservation.getAmountPaid());

            if (reservation.getRefund() != null) {
                stmt.setBigDecimal(16, reservation.getRefund());
            } else {
                stmt.setNull(16, Types.NUMERIC);
            }

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    reservation.setId(rs.getLong(1));
                }
            }
        }

        return reservation;
    }

    public void update(Reservation reservation) throws SQLException {
        String sql = """
            UPDATE reservations
            SET status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, reservation.getStatus().name());
            stmt.setBigDecimal(2, reservation.getAmountPaid());

            if (reservation.getRefund() != null) {
                stmt.setBigDecimal(3, reservation.getRefund());
            } else {
                stmt.setNull(3, Types.NUMERIC);
            }

            stmt.setLong(4, reservation.getId());
            stmt.executeUpdate();
        }
    }

    public Optional<Reservation> findById(Long id) throws SQLException {
        String sql = """
            SELECT id, room_number, room_type, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, nights,
                   subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE id = ?
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapReservation(rs));
                }
            }
        }

        return Optional.empty();
    }

    public List<Reservation> findOverlappingReservations(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        String sql = """
            SELECT id, room_number, room_type, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, nights,
                   subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE room_number = ?
              AND status != 'CANCELLED'
              AND check_in < ?
              AND check_out > ?
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        }

        return reservations;
    }

    public List<Reservation> findByNight(LocalDate night) throws SQLException {
        String sql = """
            SELECT id, room_number, room_type, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, nights,
                   subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE check_in <= ?
              AND check_out > ?
              AND (status = 'CONFIRMED' OR status = 'CHECKED_IN' OR status = 'CHECKED_OUT')
            ORDER BY room_number
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(night));
            stmt.setDate(2, Date.valueOf(night));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        }

        return reservations;
    }

    public List<Reservation> findByMonth(int year, int month) throws SQLException {
        String sql = """
            SELECT id, room_number, room_type, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, nights,
                   subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE EXTRACT(YEAR FROM check_in) = ?
              AND EXTRACT(MONTH FROM check_in) = ?
              AND (amount_paid - COALESCE(refund, 0)) > 0
            ORDER BY check_in, id
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, year);
            stmt.setInt(2, month);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        }

        return reservations;
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        Reservation reservation = new Reservation(
            rs.getLong("id"),
            rs.getInt("room_number"),
            RoomType.valueOf(rs.getString("room_type")),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            rs.getString("guest_cpf"),
            rs.getDate("check_in").toLocalDate(),
            rs.getDate("check_out").toLocalDate(),
            rs.getInt("guests"),
            rs.getString("rate_plan"),
            null
        );

        java.lang.reflect.Field statusField;
        java.lang.reflect.Field amountPaidField;
        java.lang.reflect.Field refundField;

        try {
            statusField = Reservation.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(reservation, ReservationStatus.valueOf(rs.getString("status")));

            amountPaidField = Reservation.class.getDeclaredField("amountPaid");
            amountPaidField.setAccessible(true);
            amountPaidField.set(reservation, rs.getBigDecimal("amount_paid"));

            refundField = Reservation.class.getDeclaredField("refund");
            refundField.setAccessible(true);
            BigDecimal refund = rs.getBigDecimal("refund");
            refundField.set(reservation, refund);
        } catch (Exception e) {
            throw new RuntimeException("Error mapping reservation", e);
        }

        return reservation;
    }
}
