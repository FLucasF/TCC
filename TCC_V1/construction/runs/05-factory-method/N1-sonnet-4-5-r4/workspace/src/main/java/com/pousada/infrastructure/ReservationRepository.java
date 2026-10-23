package com.pousada.infrastructure;

import com.pousada.domain.*;

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
                room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name,
                guest_passport, guest_country, check_in, check_out, guests, rate_plan,
                nightly_rate, nights, subtotal, discount, total, status, amount_paid, refund
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setReservationParameters(stmt, reservation);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    reservation.setId(rs.getLong("id"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save reservation", e);
        }

        return reservation;
    }

    public void update(Reservation reservation) {
        String sql = """
            UPDATE reservations SET
                status = ?, amount_paid = ?, refund = ?
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
        String sql = "SELECT * FROM reservations WHERE id = ?";

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservation", e);
        }

        return Optional.empty();
    }

    public List<Reservation> findByRoomAndDateRange(String roomNumber, LocalDate checkIn, LocalDate checkOut) {
        String sql = """
            SELECT * FROM reservations
            WHERE room_number = ?
            AND status != 'CANCELLED'
            AND check_in < ?
            AND check_out > ?
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = database.getConnection();
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

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservations by night", e);
        }

        return reservations;
    }

    public List<Reservation> findByMonthWithRevenue(int year, int month) {
        String sql = """
            SELECT * FROM reservations
            WHERE EXTRACT(YEAR FROM check_in) = ?
            AND EXTRACT(MONTH FROM check_in) = ?
            AND (amount_paid - COALESCE(refund, 0)) > 0
            ORDER BY check_in, id
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, year);
            stmt.setInt(2, month);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservations by month", e);
        }

        return reservations;
    }

    private void setReservationParameters(PreparedStatement stmt, Reservation reservation) throws SQLException {
        stmt.setString(1, reservation.getRoomNumber());
        stmt.setString(2, reservation.getGuestName());
        stmt.setString(3, reservation.getGuestEmail());

        GuestIdentification id = reservation.getIdentification();
        if (id instanceof GuestIdentification.Cpf cpf) {
            stmt.setString(4, cpf.cpf());
            stmt.setNull(5, Types.VARCHAR);
            stmt.setNull(6, Types.VARCHAR);
            stmt.setNull(7, Types.VARCHAR);
            stmt.setNull(8, Types.VARCHAR);
        } else if (id instanceof GuestIdentification.Cnpj cnpj) {
            stmt.setNull(4, Types.VARCHAR);
            stmt.setString(5, cnpj.cnpj());
            stmt.setString(6, cnpj.companyName());
            stmt.setNull(7, Types.VARCHAR);
            stmt.setNull(8, Types.VARCHAR);
        } else if (id instanceof GuestIdentification.Passport passport) {
            stmt.setNull(4, Types.VARCHAR);
            stmt.setNull(5, Types.VARCHAR);
            stmt.setNull(6, Types.VARCHAR);
            stmt.setString(7, passport.passport());
            stmt.setString(8, passport.country());
        }

        stmt.setDate(9, Date.valueOf(reservation.getCheckIn()));
        stmt.setDate(10, Date.valueOf(reservation.getCheckOut()));
        stmt.setInt(11, reservation.getGuests());
        stmt.setString(12, reservation.getRatePlan().name());
        stmt.setBigDecimal(13, reservation.getNightlyRate());
        stmt.setInt(14, reservation.getNights());
        stmt.setBigDecimal(15, reservation.getSubtotal());
        stmt.setBigDecimal(16, reservation.getDiscount());
        stmt.setBigDecimal(17, reservation.getTotal());
        stmt.setString(18, reservation.getStatus().name());
        stmt.setBigDecimal(19, reservation.getAmountPaid());
        if (reservation.getRefund() != null) {
            stmt.setBigDecimal(20, reservation.getRefund());
        } else {
            stmt.setNull(20, Types.DECIMAL);
        }
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        GuestIdentification identification;
        String guestCpf = rs.getString("guest_cpf");
        String companyCnpj = rs.getString("company_cnpj");
        String guestPassport = rs.getString("guest_passport");

        if (guestCpf != null) {
            identification = new GuestIdentification.Cpf(guestCpf);
        } else if (companyCnpj != null) {
            String companyName = rs.getString("company_name");
            identification = new GuestIdentification.Cnpj(companyCnpj, companyName);
        } else {
            String guestCountry = rs.getString("guest_country");
            identification = new GuestIdentification.Passport(guestPassport, guestCountry);
        }

        Reservation reservation = new Reservation(
            rs.getString("room_number"),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            identification,
            rs.getDate("check_in").toLocalDate(),
            rs.getDate("check_out").toLocalDate(),
            rs.getInt("guests"),
            RatePlan.valueOf(rs.getString("rate_plan")),
            rs.getBigDecimal("nightly_rate")
        );

        reservation.setId(rs.getLong("id"));
        reservation.setStatus(ReservationStatus.valueOf(rs.getString("status")));
        reservation.setAmountPaid(rs.getBigDecimal("amount_paid"));
        BigDecimal refund = rs.getBigDecimal("refund");
        if (!rs.wasNull()) {
            reservation.setRefund(refund);
        }

        return reservation;
    }
}
