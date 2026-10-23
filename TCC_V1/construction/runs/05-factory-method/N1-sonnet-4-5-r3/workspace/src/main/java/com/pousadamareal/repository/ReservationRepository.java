package com.pousadamareal.repository;

import com.pousadamareal.model.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    private final Database database;

    public ReservationRepository(Database database) {
        this.database = database;
    }

    public Reservation save(Reservation reservation) {
        String sql = """
            INSERT INTO reservations (
                room_number, guest_name, guest_email,
                guest_cpf, company_cnpj, company_name, guest_passport, guest_country,
                check_in, check_out, guests, rate_plan,
                nights, subtotal, discount, total,
                status, amount_paid, refund
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
        """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, reservation.getRoomNumber());
            stmt.setString(2, reservation.getGuestName());
            stmt.setString(3, reservation.getGuestEmail());
            stmt.setString(4, reservation.getIdentification().getGuestCpf());
            stmt.setString(5, reservation.getIdentification().getCompanyCnpj());
            stmt.setString(6, reservation.getIdentification().getCompanyName());
            stmt.setString(7, reservation.getIdentification().getGuestPassport());
            stmt.setString(8, reservation.getIdentification().getGuestCountry());
            stmt.setDate(9, Date.valueOf(reservation.getCheckIn()));
            stmt.setDate(10, Date.valueOf(reservation.getCheckOut()));
            stmt.setInt(11, reservation.getGuests());
            stmt.setString(12, reservation.getRatePlan().name());
            stmt.setInt(13, reservation.getNights());
            stmt.setBigDecimal(14, reservation.getSubtotal());
            stmt.setBigDecimal(15, reservation.getDiscount());
            stmt.setBigDecimal(16, reservation.getTotal());
            stmt.setString(17, reservation.getStatus().name());
            stmt.setBigDecimal(18, reservation.getAmountPaid());
            stmt.setBigDecimal(19, reservation.getRefund());

            var rs = stmt.executeQuery();
            if (rs.next()) {
                long id = rs.getLong(1);
                return new Reservation(
                    id,
                    reservation.getRoomNumber(),
                    reservation.getGuestName(),
                    reservation.getGuestEmail(),
                    reservation.getIdentification(),
                    reservation.getCheckIn(),
                    reservation.getCheckOut(),
                    reservation.getGuests(),
                    reservation.getRatePlan(),
                    reservation.getNights(),
                    reservation.getSubtotal(),
                    reservation.getDiscount(),
                    reservation.getTotal(),
                    reservation.getStatus(),
                    reservation.getAmountPaid(),
                    reservation.getRefund()
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save reservation", e);
        }
        throw new RuntimeException("Failed to save reservation");
    }

    public void update(Reservation reservation) {
        String sql = """
            UPDATE reservations SET
                status = ?,
                amount_paid = ?,
                refund = ?
            WHERE id = ?
        """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, reservation.getStatus().name());
            stmt.setBigDecimal(2, reservation.getAmountPaid());
            stmt.setBigDecimal(3, reservation.getRefund());
            stmt.setLong(4, reservation.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation", e);
        }
    }

    public Optional<Reservation> findById(long id) {
        String sql = """
            SELECT * FROM reservations WHERE id = ?
        """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            var rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(buildReservation(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservation", e);
        }
        return Optional.empty();
    }

    public List<Reservation> findConflicting(String roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeId) {
        String sql = """
            SELECT * FROM reservations
            WHERE room_number = ?
            AND status != 'CANCELLED'
            AND check_in < ?
            AND check_out > ?
        """ + (excludeId != null ? " AND id != ?" : "");

        List<Reservation> conflicts = new ArrayList<>();
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));
            if (excludeId != null) {
                stmt.setLong(4, excludeId);
            }

            var rs = stmt.executeQuery();
            while (rs.next()) {
                conflicts.add(buildReservation(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check conflicts", e);
        }
        return conflicts;
    }

    public List<Reservation> findByCheckInMonth(int year, int month) {
        String sql = """
            SELECT * FROM reservations
            WHERE EXTRACT(YEAR FROM check_in) = ?
            AND EXTRACT(MONTH FROM check_in) = ?
            ORDER BY check_in, id
        """;

        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, year);
            stmt.setInt(2, month);

            var rs = stmt.executeQuery();
            while (rs.next()) {
                reservations.add(buildReservation(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservations", e);
        }
        return reservations;
    }

    public List<Reservation> findByNight(LocalDate night) {
        String sql = """
            SELECT * FROM reservations
            WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            AND check_in <= ?
            AND check_out > ?
            ORDER BY room_number
        """;

        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(night));
            stmt.setDate(2, Date.valueOf(night));

            var rs = stmt.executeQuery();
            while (rs.next()) {
                reservations.add(buildReservation(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservations", e);
        }
        return reservations;
    }

    private Reservation buildReservation(ResultSet rs) throws SQLException {
        GuestIdentification identification;
        String cpf = rs.getString("guest_cpf");
        String cnpj = rs.getString("company_cnpj");
        String passport = rs.getString("guest_passport");

        if (cpf != null) {
            identification = new CpfIdentification(cpf);
        } else if (cnpj != null) {
            identification = new CnpjIdentification(cnpj, rs.getString("company_name"));
        } else {
            identification = new PassportIdentification(passport, rs.getString("guest_country"));
        }

        return new Reservation(
            rs.getLong("id"),
            rs.getString("room_number"),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            identification,
            rs.getDate("check_in").toLocalDate(),
            rs.getDate("check_out").toLocalDate(),
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
