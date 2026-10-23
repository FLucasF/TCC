package com.marealta.infrastructure.database;

import com.marealta.domain.model.*;

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

    public Reservation save(Reservation reservation) throws SQLException {
        String sql = """
            INSERT INTO reservations (
                room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name,
                guest_passport, guest_country, check_in, check_out, guests, rate_plan,
                nights, subtotal, discount, total, status, amount_paid, refund
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setReservationParameters(stmt, reservation);
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
            UPDATE reservations SET
                status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, reservation.getStatus().name());
            stmt.setBigDecimal(2, reservation.getAmountPaid());

            if (reservation.getRefund() != null) {
                stmt.setBigDecimal(3, reservation.getRefund());
            } else {
                stmt.setNull(3, Types.DECIMAL);
            }

            stmt.setLong(4, reservation.getId());
            stmt.executeUpdate();
        }
    }

    public Optional<Reservation> findById(Long id) throws SQLException {
        String sql = "SELECT * FROM reservations WHERE id = ?";

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

    public List<Reservation> findAll() throws SQLException {
        String sql = "SELECT * FROM reservations ORDER BY id";
        List<Reservation> reservations = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                reservations.add(mapReservation(rs));
            }
        }

        return reservations;
    }

    public boolean hasConflictingReservation(String roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeId) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM reservations
            WHERE room_number = ?
            AND status != 'CANCELLED'
            AND check_in < ?
            AND check_out > ?
            AND (? IS NULL OR id != ?)
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));
            stmt.setObject(4, excludeId);
            stmt.setObject(5, excludeId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }

        return false;
    }

    private void setReservationParameters(PreparedStatement stmt, Reservation reservation) throws SQLException {
        GuestIdentification guest = reservation.getGuest();

        stmt.setString(1, reservation.getRoomNumber());
        stmt.setString(2, guest.getGuestName());
        stmt.setString(3, guest.getGuestEmail());

        if (guest instanceof CpfGuest cpf) {
            stmt.setString(4, cpf.guestCpf());
            stmt.setNull(5, Types.VARCHAR);
            stmt.setNull(6, Types.VARCHAR);
            stmt.setNull(7, Types.VARCHAR);
            stmt.setNull(8, Types.VARCHAR);
        } else if (guest instanceof CompanyGuest company) {
            stmt.setNull(4, Types.VARCHAR);
            stmt.setString(5, company.companyCnpj());
            stmt.setString(6, company.companyName());
            stmt.setNull(7, Types.VARCHAR);
            stmt.setNull(8, Types.VARCHAR);
        } else if (guest instanceof PassportGuest passport) {
            stmt.setNull(4, Types.VARCHAR);
            stmt.setNull(5, Types.VARCHAR);
            stmt.setNull(6, Types.VARCHAR);
            stmt.setString(7, passport.guestPassport());
            stmt.setString(8, passport.guestCountry());
        }

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

        if (reservation.getRefund() != null) {
            stmt.setBigDecimal(19, reservation.getRefund());
        } else {
            stmt.setNull(19, Types.DECIMAL);
        }
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        GuestIdentification guest = createGuest(rs);

        Reservation reservation = new Reservation(
            rs.getLong("id"),
            rs.getString("room_number"),
            guest,
            rs.getDate("check_in").toLocalDate(),
            rs.getDate("check_out").toLocalDate(),
            rs.getInt("guests"),
            RatePlan.valueOf(rs.getString("rate_plan")),
            rs.getInt("nights"),
            rs.getBigDecimal("subtotal"),
            rs.getBigDecimal("discount"),
            rs.getBigDecimal("total")
        );

        String status = rs.getString("status");
        BigDecimal amountPaid = rs.getBigDecimal("amount_paid");
        BigDecimal refund = rs.getBigDecimal("refund");

        if (!status.equals("PENDING")) {
            setReservationState(reservation, status, amountPaid, refund);
        }

        return reservation;
    }

    private GuestIdentification createGuest(ResultSet rs) throws SQLException {
        String guestCpf = rs.getString("guest_cpf");
        String companyCnpj = rs.getString("company_cnpj");
        String guestPassport = rs.getString("guest_passport");

        String guestName = rs.getString("guest_name");
        String guestEmail = rs.getString("guest_email");

        if (guestCpf != null) {
            return new CpfGuest(guestName, guestEmail, guestCpf);
        } else if (companyCnpj != null) {
            return new CompanyGuest(guestName, guestEmail, companyCnpj, rs.getString("company_name"));
        } else if (guestPassport != null) {
            return new PassportGuest(guestName, guestEmail, guestPassport, rs.getString("guest_country"));
        }

        throw new IllegalStateException("Invalid guest identification data");
    }

    private void setReservationState(Reservation reservation, String status, BigDecimal amountPaid, BigDecimal refund) {
        try {
            java.lang.reflect.Field statusField = Reservation.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(reservation, ReservationStatus.valueOf(status));

            java.lang.reflect.Field amountPaidField = Reservation.class.getDeclaredField("amountPaid");
            amountPaidField.setAccessible(true);
            amountPaidField.set(reservation, amountPaid);

            if (refund != null) {
                java.lang.reflect.Field refundField = Reservation.class.getDeclaredField("refund");
                refundField.setAccessible(true);
                refundField.set(reservation, refund);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to set reservation state", e);
        }
    }
}
