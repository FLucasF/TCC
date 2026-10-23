package com.pousadamarealta.repository;

import com.pousadamarealta.database.Database;
import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;

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

    public Reservation save(Reservation reservation) throws SQLException {
        if (reservation.getId() == null) {
            return insert(reservation);
        } else {
            return update(reservation);
        }
    }

    private Reservation insert(Reservation reservation) throws SQLException {
        String sql = """
            INSERT INTO reservations (
                room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name,
                guest_passport, guest_country, check_in, check_out, guests, rate_plan,
                status, amount_paid, refund
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
        }

        return reservation;
    }

    private Reservation update(Reservation reservation) throws SQLException {
        String sql = """
            UPDATE reservations SET
                room_number = ?, guest_name = ?, guest_email = ?, guest_cpf = ?,
                company_cnpj = ?, company_name = ?, guest_passport = ?, guest_country = ?,
                check_in = ?, check_out = ?, guests = ?, rate_plan = ?,
                status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
        """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setReservationParameters(stmt, reservation);
            stmt.setLong(16, reservation.getId());
            stmt.executeUpdate();
        }

        return reservation;
    }

    private void setReservationParameters(PreparedStatement stmt, Reservation r) throws SQLException {
        stmt.setString(1, r.getRoomNumber());
        stmt.setString(2, r.getGuestName());
        stmt.setString(3, r.getGuestEmail());
        stmt.setString(4, r.getGuestCpf());
        stmt.setString(5, r.getCompanyCnpj());
        stmt.setString(6, r.getCompanyName());
        stmt.setString(7, r.getGuestPassport());
        stmt.setString(8, r.getGuestCountry());
        stmt.setDate(9, Date.valueOf(r.getCheckIn()));
        stmt.setDate(10, Date.valueOf(r.getCheckOut()));
        stmt.setInt(11, r.getGuests());
        stmt.setString(12, r.getRatePlan().name());
        stmt.setString(13, r.getStatus().name());
        stmt.setBigDecimal(14, r.getAmountPaid());
        stmt.setBigDecimal(15, r.getRefund());
    }

    public Optional<Reservation> findById(Long id) throws SQLException {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, company_cnpj,
                   company_name, guest_passport, guest_country, check_in, check_out,
                   guests, rate_plan, status, amount_paid, refund
            FROM reservations
            WHERE id = ?
        """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapReservation(rs));
                }
            }
        }

        return Optional.empty();
    }

    public boolean hasConflict(String roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeId) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM reservations
            WHERE room_number = ?
              AND status != 'CANCELLED'
              AND check_in < ?
              AND check_out > ?
              AND (? IS NULL OR id != ?)
        """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));
            if (excludeId != null) {
                stmt.setLong(4, excludeId);
                stmt.setLong(5, excludeId);
            } else {
                stmt.setNull(4, Types.BIGINT);
                stmt.setNull(5, Types.BIGINT);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }

        return false;
    }

    public List<Reservation> findByRoomAndDate(String roomNumber, LocalDate date) throws SQLException {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, company_cnpj,
                   company_name, guest_passport, guest_country, check_in, check_out,
                   guests, rate_plan, status, amount_paid, refund
            FROM reservations
            WHERE room_number = ?
              AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
              AND check_in <= ?
              AND check_out > ?
            ORDER BY id
        """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomNumber);
            stmt.setDate(2, Date.valueOf(date));
            stmt.setDate(3, Date.valueOf(date));

            List<Reservation> reservations = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }

            return reservations;
        }
    }

    public List<Reservation> findByMonth(int year, int month) throws SQLException {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, company_cnpj,
                   company_name, guest_passport, guest_country, check_in, check_out,
                   guests, rate_plan, status, amount_paid, refund
            FROM reservations
            WHERE EXTRACT(YEAR FROM check_in) = ?
              AND EXTRACT(MONTH FROM check_in) = ?
              AND (amount_paid > refund OR (refund IS NULL AND amount_paid > 0))
            ORDER BY check_in, id
        """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, year);
            stmt.setInt(2, month);

            List<Reservation> reservations = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }

            return reservations;
        }
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        Reservation reservation = new Reservation();
        reservation.setId(rs.getLong("id"));
        reservation.setRoomNumber(rs.getString("room_number"));
        reservation.setGuestName(rs.getString("guest_name"));
        reservation.setGuestEmail(rs.getString("guest_email"));
        reservation.setGuestCpf(rs.getString("guest_cpf"));
        reservation.setCompanyCnpj(rs.getString("company_cnpj"));
        reservation.setCompanyName(rs.getString("company_name"));
        reservation.setGuestPassport(rs.getString("guest_passport"));
        reservation.setGuestCountry(rs.getString("guest_country"));
        reservation.setCheckIn(rs.getDate("check_in").toLocalDate());
        reservation.setCheckOut(rs.getDate("check_out").toLocalDate());
        reservation.setGuests(rs.getInt("guests"));
        reservation.setRatePlan(RatePlan.valueOf(rs.getString("rate_plan")));
        reservation.setStatus(ReservationStatus.valueOf(rs.getString("status")));
        reservation.setAmountPaid(rs.getBigDecimal("amount_paid"));

        BigDecimal refund = rs.getBigDecimal("refund");
        if (!rs.wasNull()) {
            reservation.setRefund(refund);
        }

        return reservation;
    }
}
