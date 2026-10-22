package com.pousada.maralta.repository;

import com.pousada.maralta.domain.*;
import com.pousada.maralta.infrastructure.DatabaseConfig;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcReservationRepository implements ReservationRepository {
    @Override
    public Reservation save(Reservation reservation) {
        if (reservation.getId() == null) {
            return insert(reservation);
        } else {
            return update(reservation);
        }
    }

    private Reservation insert(Reservation reservation) {
        String sql = """
            INSERT INTO reservations (
                room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name,
                guest_passport, guest_country, check_in, check_out, guests, rate_plan,
                nights, subtotal, discount, total, status, amount_paid, refund
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            setReservationParameters(stmt, reservation);
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    reservation.setId(rs.getLong(1));
                }
            }

            return reservation;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert reservation", e);
        }
    }

    private Reservation update(Reservation reservation) {
        String sql = """
            UPDATE reservations SET
                room_number = ?, guest_name = ?, guest_email = ?, guest_cpf = ?,
                company_cnpj = ?, company_name = ?, guest_passport = ?, guest_country = ?,
                check_in = ?, check_out = ?, guests = ?, rate_plan = ?,
                nights = ?, subtotal = ?, discount = ?, total = ?, status = ?,
                amount_paid = ?, refund = ?
            WHERE id = ?
        """;

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setReservationParameters(stmt, reservation);
            stmt.setLong(20, reservation.getId());
            stmt.executeUpdate();

            return reservation;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation", e);
        }
    }

    @Override
    public Optional<Reservation> findById(Long id) {
        String sql = "SELECT * FROM reservations WHERE id = ?";

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservation", e);
        }

        return Optional.empty();
    }

    @Override
    public List<Reservation> findByRoomAndNight(String roomNumber, LocalDate night) {
        String sql = """
            SELECT * FROM reservations
            WHERE room_number = ? AND check_in <= ? AND check_out > ? AND status != 'CANCELLED'
        """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomNumber);
            stmt.setDate(2, Date.valueOf(night));
            stmt.setDate(3, Date.valueOf(night));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservations", e);
        }

        return reservations;
    }

    @Override
    public List<Reservation> findPaidByNight(LocalDate night) {
        String sql = """
            SELECT * FROM reservations
            WHERE check_in <= ? AND check_out > ? AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            ORDER BY room_number
        """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(night));
            stmt.setDate(2, Date.valueOf(night));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find paid reservations", e);
        }

        return reservations;
    }

    @Override
    public List<Reservation> findByCheckInMonth(YearMonth month) {
        String sql = """
            SELECT * FROM reservations
            WHERE EXTRACT(YEAR FROM check_in) = ? AND EXTRACT(MONTH FROM check_in) = ?
            ORDER BY check_in, id
        """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, month.getYear());
            stmt.setInt(2, month.getMonthValue());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservations by month", e);
        }

        return reservations;
    }

    private void setReservationParameters(PreparedStatement stmt, Reservation reservation) throws SQLException {
        stmt.setString(1, reservation.getRoomNumber());
        stmt.setString(2, reservation.getGuestName());
        stmt.setString(3, reservation.getGuestEmail());

        GuestIdentification id = reservation.getIdentification();
        if (id instanceof CpfIdentification cpf) {
            stmt.setString(4, cpf.cpf());
            stmt.setNull(5, Types.VARCHAR);
            stmt.setNull(6, Types.VARCHAR);
            stmt.setNull(7, Types.VARCHAR);
            stmt.setNull(8, Types.VARCHAR);
        } else if (id instanceof CnpjIdentification cnpj) {
            stmt.setNull(4, Types.VARCHAR);
            stmt.setString(5, cnpj.cnpj());
            stmt.setString(6, cnpj.companyName());
            stmt.setNull(7, Types.VARCHAR);
            stmt.setNull(8, Types.VARCHAR);
        } else if (id instanceof PassportIdentification passport) {
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

    private Reservation mapRow(ResultSet rs) throws SQLException {
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

        BigDecimal refund = rs.getBigDecimal("refund");

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
            refund
        );
    }
}
