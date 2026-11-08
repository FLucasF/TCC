package com.pousadamarealta.repository;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.notification.ReservationObserver;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    private final DataSource dataSource;
    private final List<ReservationObserver> observers;

    public ReservationRepository(DataSource dataSource, List<ReservationObserver> observers) {
        this.dataSource = dataSource;
        this.observers = observers;
    }

    public void save(Reservation reservation) {
        if (reservation.getId() == null) {
            insert(reservation);
        } else {
            update(reservation);
        }
    }

    private void insert(Reservation reservation) {
        String sql = """
            INSERT INTO reservations (
                room_number, room_type, guest_name, guest_email, guest_cpf,
                check_in, check_out, guests, rate_plan, nights, subtotal, discount, total,
                status, amount_paid, refund
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            setReservationParameters(stmt, reservation);

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    reservation.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error saving reservation", e);
        }
    }

    private void update(Reservation reservation) {
        String sql = """
            UPDATE reservations SET
                status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, reservation.getStatus().name());
            stmt.setBigDecimal(2, reservation.getAmountPaid());
            stmt.setBigDecimal(3, reservation.getRefund());
            stmt.setLong(4, reservation.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating reservation", e);
        }
    }

    private void setReservationParameters(PreparedStatement stmt, Reservation reservation) throws SQLException {
        stmt.setString(1, reservation.getRoomNumber());
        stmt.setString(2, reservation.getRoomType().name());
        stmt.setString(3, reservation.getGuestName());
        stmt.setString(4, reservation.getGuestEmail());
        stmt.setString(5, reservation.getGuestCpf());
        stmt.setDate(6, Date.valueOf(reservation.getCheckIn()));
        stmt.setDate(7, Date.valueOf(reservation.getCheckOut()));
        stmt.setInt(8, reservation.getGuests());
        stmt.setString(9, reservation.getRatePlan().name());
        stmt.setInt(10, reservation.getNights());
        stmt.setBigDecimal(11, reservation.getSubtotal());
        stmt.setBigDecimal(12, reservation.getDiscount());
        stmt.setBigDecimal(13, reservation.getTotal());
        stmt.setString(14, reservation.getStatus().name());
        stmt.setBigDecimal(15, reservation.getAmountPaid());
        stmt.setBigDecimal(16, reservation.getRefund());
    }

    public Optional<Reservation> findById(Long id) {
        String sql = """
            SELECT id, room_number, room_type, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, nights, subtotal, discount, total,
                   status, amount_paid, refund
            FROM reservations
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding reservation", e);
        }

        return Optional.empty();
    }

    public List<Reservation> findByRoomAndDateRange(String roomNumber, LocalDate checkIn, LocalDate checkOut) {
        String sql = """
            SELECT id, room_number, room_type, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, nights, subtotal, discount, total,
                   status, amount_paid, refund
            FROM reservations
            WHERE room_number = ?
              AND status != 'CANCELLED'
              AND check_in < ?
              AND check_out > ?
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding reservations", e);
        }

        return reservations;
    }

    public List<Reservation> findPaidReservationsForNight(LocalDate night) {
        String sql = """
            SELECT id, room_number, room_type, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, nights, subtotal, discount, total,
                   status, amount_paid, refund
            FROM reservations
            WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
              AND check_in <= ?
              AND check_out > ?
            ORDER BY room_number
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(night));
            stmt.setDate(2, Date.valueOf(night));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding reservations for night", e);
        }

        return reservations;
    }

    public List<Reservation> findReservationsForRevenueReport(int year, int month) {
        String sql = """
            SELECT id, room_number, room_type, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, nights, subtotal, discount, total,
                   status, amount_paid, refund
            FROM reservations
            WHERE EXTRACT(YEAR FROM check_in) = ?
              AND EXTRACT(MONTH FROM check_in) = ?
              AND amount_paid - COALESCE(refund, 0) > 0
            ORDER BY check_in, id
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, year);
            stmt.setInt(2, month);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding reservations for revenue report", e);
        }

        return reservations;
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        Reservation reservation = new Reservation(
            rs.getString("room_number"),
            RoomType.valueOf(rs.getString("room_type")),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            rs.getString("guest_cpf"),
            rs.getDate("check_in").toLocalDate(),
            rs.getDate("check_out").toLocalDate(),
            rs.getInt("guests"),
            RatePlan.valueOf(rs.getString("rate_plan"))
        );

        reservation.setId(rs.getLong("id"));
        reservation.setStatus(ReservationStatus.valueOf(rs.getString("status")));
        reservation.setAmountPaid(rs.getBigDecimal("amount_paid"));

        BigDecimal refund = rs.getBigDecimal("refund");
        reservation.setRefund(refund);

        for (ReservationObserver observer : observers) {
            reservation.addObserver(observer);
        }

        return reservation;
    }
}
