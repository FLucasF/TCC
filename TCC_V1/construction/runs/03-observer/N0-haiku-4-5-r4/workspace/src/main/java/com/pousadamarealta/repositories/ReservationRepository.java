package com.pousadamarealta.repositories;

import com.pousadamarealta.database.DatabaseConfig;
import com.pousadamarealta.models.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    private final DatabaseConfig dbConfig;

    public ReservationRepository(DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    public Reservation save(Reservation reservation) throws SQLException {
        try (Connection conn = dbConfig.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                """
                INSERT INTO reservations
                (room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                 guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """,
                Statement.RETURN_GENERATED_KEYS
            )) {
                stmt.setInt(1, reservation.getRoomNumber());
                stmt.setString(2, reservation.getGuestName());
                stmt.setString(3, reservation.getGuestEmail());
                stmt.setString(4, reservation.getGuestCpf());
                stmt.setDate(5, Date.valueOf(reservation.getCheckIn()));
                stmt.setDate(6, Date.valueOf(reservation.getCheckOut()));
                stmt.setInt(7, reservation.getGuests());
                stmt.setString(8, reservation.getRatePlan().toString());
                stmt.setInt(9, reservation.getNights());
                stmt.setBigDecimal(10, reservation.getSubtotal());
                stmt.setBigDecimal(11, reservation.getDiscount());
                stmt.setBigDecimal(12, reservation.getTotal());
                stmt.setString(13, reservation.getStatus().toString());
                stmt.setBigDecimal(14, reservation.getAmountPaid());
                stmt.setObject(15, reservation.getRefund());

                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return new Reservation(
                            rs.getInt("id"),
                            reservation.getRoomNumber(),
                            reservation.getGuestName(),
                            reservation.getGuestEmail(),
                            reservation.getGuestCpf(),
                            reservation.getCheckIn(),
                            reservation.getCheckOut(),
                            reservation.getGuests(),
                            reservation.getRatePlan(),
                            reservation.getNights(),
                            reservation.getSubtotal(),
                            reservation.getDiscount(),
                            reservation.getTotal()
                        );
                    }
                }
            }
        }
        return reservation;
    }

    public Optional<Reservation> findById(int id) throws SQLException {
        try (Connection conn = dbConfig.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                """
                SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                       guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
                FROM reservations WHERE id = ?
                """
            )) {
                stmt.setInt(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapReservation(rs));
                    }
                }
            }
        }
        return Optional.empty();
    }

    public void update(Reservation reservation) throws SQLException {
        try (Connection conn = dbConfig.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                """
                UPDATE reservations
                SET status = ?, amount_paid = ?, refund = ?
                WHERE id = ?
                """
            )) {
                stmt.setString(1, reservation.getStatus().toString());
                stmt.setBigDecimal(2, reservation.getAmountPaid());
                stmt.setObject(3, reservation.getRefund());
                stmt.setInt(4, reservation.getId());
                stmt.executeUpdate();
            }
        }
    }

    public List<Reservation> findByRoomAndDateRange(int roomNumber, LocalDate startDate, LocalDate endDate) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = dbConfig.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                """
                SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                       guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
                FROM reservations
                WHERE room_number = ?
                  AND status != 'CANCELLED'
                  AND check_in < ? AND check_out > ?
                """
            )) {
                stmt.setInt(1, roomNumber);
                stmt.setDate(2, Date.valueOf(endDate));
                stmt.setDate(3, Date.valueOf(startDate));
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        reservations.add(mapReservation(rs));
                    }
                }
            }
        }
        return reservations;
    }

    public List<Reservation> findByMonthWithRevenue(int year, int month) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = dbConfig.getConnection()) {
            LocalDate startOfMonth = LocalDate.of(year, month, 1);
            LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);

            try (PreparedStatement stmt = conn.prepareStatement(
                """
                SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                       guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
                FROM reservations
                WHERE check_in >= ? AND check_in <= ?
                ORDER BY check_in, id
                """
            )) {
                stmt.setDate(1, Date.valueOf(startOfMonth));
                stmt.setDate(2, Date.valueOf(endOfMonth));
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Reservation res = mapReservation(rs);
                        BigDecimal netAmount = res.getAmountPaid();
                        if (res.getRefund() != null) {
                            netAmount = netAmount.subtract(res.getRefund());
                        }
                        if (netAmount.compareTo(BigDecimal.ZERO) > 0) {
                            reservations.add(res);
                        }
                    }
                }
            }
        }
        return reservations;
    }

    public List<Reservation> findByDateAndConfirmed(LocalDate date) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = dbConfig.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                """
                SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                       guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
                FROM reservations
                WHERE check_in <= ? AND check_out > ?
                  AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
                ORDER BY room_number
                """
            )) {
                stmt.setDate(1, Date.valueOf(date));
                stmt.setDate(2, Date.valueOf(date));
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        reservations.add(mapReservation(rs));
                    }
                }
            }
        }
        return reservations;
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        Reservation res = new Reservation(
            rs.getInt("id"),
            rs.getInt("room_number"),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            rs.getString("guest_cpf"),
            rs.getDate("check_in").toLocalDate(),
            rs.getDate("check_out").toLocalDate(),
            rs.getInt("guests"),
            RatePlan.valueOf(rs.getString("rate_plan")),
            rs.getInt("nights"),
            rs.getBigDecimal("subtotal"),
            rs.getBigDecimal("discount"),
            rs.getBigDecimal("total")
        );
        res.setStatus(ReservationStatus.valueOf(rs.getString("status")));
        res.setAmountPaid(rs.getBigDecimal("amount_paid"));
        res.setRefund(rs.getObject("refund", BigDecimal.class));
        return res;
    }
}
