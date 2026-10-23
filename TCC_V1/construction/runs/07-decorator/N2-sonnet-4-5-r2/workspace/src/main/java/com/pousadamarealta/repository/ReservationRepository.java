package com.pousadamarealta.repository;

import com.pousadamarealta.database.DatabaseManager;
import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ReservationRepository {
    private final DatabaseManager dbManager;

    public ReservationRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public Reservation save(Reservation reservation) {
        if (reservation.getId() == null) {
            return insert(reservation);
        } else {
            update(reservation);
            return reservation;
        }
    }

    private Reservation insert(Reservation reservation) {
        String sql = """
            INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf,
                                     check_in, check_out, guests, rate_plan, nightly_rate,
                                     status, amount_paid, refund)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, reservation.getRoomNumber());
            stmt.setString(2, reservation.getGuestName());
            stmt.setString(3, reservation.getGuestEmail());
            stmt.setString(4, reservation.getGuestCpf());
            stmt.setDate(5, Date.valueOf(reservation.getCheckIn()));
            stmt.setDate(6, Date.valueOf(reservation.getCheckOut()));
            stmt.setInt(7, reservation.getGuests());
            stmt.setString(8, reservation.getRatePlan().name());
            stmt.setBigDecimal(9, reservation.getNightlyRate());
            stmt.setString(10, reservation.getStatus().name());
            stmt.setBigDecimal(11, reservation.getAmountPaid());
            if (reservation.getRefund() != null) {
                stmt.setBigDecimal(12, reservation.getRefund());
            } else {
                stmt.setNull(12, Types.DECIMAL);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id");
                    return new Reservation(id, reservation.getRoomNumber(), reservation.getGuestName(),
                                         reservation.getGuestEmail(), reservation.getGuestCpf(),
                                         reservation.getCheckIn(), reservation.getCheckOut(),
                                         reservation.getGuests(), reservation.getRatePlan(),
                                         reservation.getNightlyRate(), reservation.getStatus(),
                                         reservation.getAmountPaid(), reservation.getRefund());
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert reservation", e);
        }

        throw new RuntimeException("Failed to get generated ID");
    }

    private void update(Reservation reservation) {
        String sql = """
            UPDATE reservations SET
                status = ?,
                amount_paid = ?,
                refund = ?
            WHERE id = ?
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, reservation.getStatus().name());
            stmt.setBigDecimal(2, reservation.getAmountPaid());
            if (reservation.getRefund() != null) {
                stmt.setBigDecimal(3, reservation.getRefund());
            } else {
                stmt.setNull(3, Types.DECIMAL);
            }
            stmt.setLong(4, reservation.getId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation", e);
        }
    }

    public Optional<Reservation> findById(Long id) {
        String sql = "SELECT * FROM reservations WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(buildReservation(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservation", e);
        }

        return Optional.empty();
    }

    public List<Reservation> findConflicting(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        String sql = """
            SELECT * FROM reservations
            WHERE room_number = ?
              AND status != 'CANCELLED'
              AND check_in < ?
              AND check_out > ?
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(buildReservation(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find conflicting reservations", e);
        }

        return reservations;
    }

    public List<Reservation> findByNightAndPaid(LocalDate night) {
        String sql = """
            SELECT * FROM reservations
            WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
              AND check_in <= ?
              AND check_out > ?
            ORDER BY room_number
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(night));
            stmt.setDate(2, Date.valueOf(night));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(buildReservation(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservations by night", e);
        }

        return reservations;
    }

    public List<Reservation> findByMonth(int year, int month) {
        String sql = """
            SELECT * FROM reservations
            WHERE EXTRACT(YEAR FROM check_in) = ?
              AND EXTRACT(MONTH FROM check_in) = ?
              AND (amount_paid > 0 OR refund IS NOT NULL)
            ORDER BY check_in, id
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, year);
            stmt.setInt(2, month);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(buildReservation(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservations by month", e);
        }

        return reservations;
    }

    private Reservation buildReservation(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        int roomNumber = rs.getInt("room_number");
        String guestName = rs.getString("guest_name");
        String guestEmail = rs.getString("guest_email");
        String guestCpf = rs.getString("guest_cpf");
        LocalDate checkIn = rs.getDate("check_in").toLocalDate();
        LocalDate checkOut = rs.getDate("check_out").toLocalDate();
        int guests = rs.getInt("guests");
        RatePlan ratePlan = RatePlan.valueOf(rs.getString("rate_plan"));
        BigDecimal nightlyRate = rs.getBigDecimal("nightly_rate");
        ReservationStatus status = ReservationStatus.valueOf(rs.getString("status"));
        BigDecimal amountPaid = rs.getBigDecimal("amount_paid");
        BigDecimal refund = rs.getBigDecimal("refund");

        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf,
                             checkIn, checkOut, guests, ratePlan, nightlyRate,
                             status, amountPaid, refund);
    }
}
