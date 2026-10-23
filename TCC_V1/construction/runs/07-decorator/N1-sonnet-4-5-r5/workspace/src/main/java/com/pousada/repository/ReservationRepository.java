package com.pousada.repository;

import com.pousada.config.Database;
import com.pousada.domain.RatePlan;
import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    private final Database database;

    public ReservationRepository(Database database) {
        this.database = database;
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
            INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                                      guests, rate_plan, nightly_rate, status, amount_paid, refund)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, reservation.getRoomNumber());
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

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    reservation.setId(rs.getLong(1));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert reservation", e);
        }

        return reservation;
    }

    private void update(Reservation reservation) {
        String sql = """
            UPDATE reservations
            SET status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
            """;

        try (Connection conn = database.getConnection();
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
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                   guests, rate_plan, nightly_rate, status, amount_paid, refund
            FROM reservations
            WHERE id = ?
            """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservation by id", e);
        }

        return Optional.empty();
    }

    public List<Reservation> findByRoomAndDateRange(String roomNumber, LocalDate from, LocalDate to) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                   guests, rate_plan, nightly_rate, status, amount_paid, refund
            FROM reservations
            WHERE room_number = ?
              AND check_in < ?
              AND check_out > ?
              AND status != 'CANCELLED'
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomNumber);
            stmt.setDate(2, Date.valueOf(to));
            stmt.setDate(3, Date.valueOf(from));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservations by room and date range", e);
        }

        return reservations;
    }

    public List<Reservation> findPaidByDate(LocalDate date) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                   guests, rate_plan, nightly_rate, status, amount_paid, refund
            FROM reservations
            WHERE check_in <= ?
              AND check_out > ?
              AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            ORDER BY room_number
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(date));
            stmt.setDate(2, Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find paid reservations by date", e);
        }

        return reservations;
    }

    public List<Reservation> findRevenueByMonth(YearMonth month) {
        LocalDate startOfMonth = month.atDay(1);
        LocalDate startOfNextMonth = month.plusMonths(1).atDay(1);

        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                   guests, rate_plan, nightly_rate, status, amount_paid, refund
            FROM reservations
            WHERE check_in >= ?
              AND check_in < ?
              AND (status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
                   OR (status = 'CANCELLED' AND rate_plan = 'NON_REFUNDABLE' AND amount_paid > 0))
            ORDER BY check_in, id
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(startOfMonth));
            stmt.setDate(2, Date.valueOf(startOfNextMonth));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find revenue by month", e);
        }

        return reservations;
    }

    private Reservation mapRow(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String roomNumber = rs.getString("room_number");
        String guestName = rs.getString("guest_name");
        String guestEmail = rs.getString("guest_email");
        String guestCpf = rs.getString("guest_cpf");
        LocalDate checkIn = rs.getDate("check_in").toLocalDate();
        LocalDate checkOut = rs.getDate("check_out").toLocalDate();
        int guests = rs.getInt("guests");
        RatePlan ratePlan = RatePlan.valueOf(rs.getString("rate_plan"));
        ReservationStatus status = ReservationStatus.valueOf(rs.getString("status"));

        return new Reservation(
            id, roomNumber, guestName, guestEmail, guestCpf,
            checkIn, checkOut, guests, ratePlan,
            rs.getBigDecimal("nightly_rate"), status,
            rs.getBigDecimal("amount_paid"),
            rs.getBigDecimal("refund")
        );
    }
}
