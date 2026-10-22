package com.pousada.repository;

import com.pousada.db.Database;
import com.pousada.domain.RatePlan;
import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    private static final ReservationRepository instance = new ReservationRepository();

    private ReservationRepository() {
    }

    public static ReservationRepository getInstance() {
        return instance;
    }

    public Reservation save(Reservation reservation) {
        try (Connection conn = Database.getInstance().getConnection()) {
            var ps = conn.prepareStatement(
                """
                INSERT INTO reservations (
                    room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name,
                    guest_passport, guest_country, check_in, check_out, guests, rate_plan, nights,
                    subtotal, discount, total, status, amount_paid, refund
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """,
                java.sql.Statement.RETURN_GENERATED_KEYS
            );

            ps.setInt(1, reservation.getRoomNumber());
            ps.setString(2, reservation.getGuestName());
            ps.setString(3, reservation.getGuestEmail());
            ps.setString(4, reservation.getGuestCpf());
            ps.setString(5, reservation.getCompanyCnpj());
            ps.setString(6, reservation.getCompanyName());
            ps.setString(7, reservation.getGuestPassport());
            ps.setString(8, reservation.getGuestCountry());
            ps.setObject(9, reservation.getCheckIn());
            ps.setObject(10, reservation.getCheckOut());
            ps.setInt(11, reservation.getGuests());
            ps.setString(12, reservation.getRatePlan().name());
            ps.setInt(13, reservation.getNights());
            ps.setBigDecimal(14, reservation.getSubtotal());
            ps.setBigDecimal(15, reservation.getDiscount());
            ps.setBigDecimal(16, reservation.getTotal());
            ps.setString(17, reservation.getStatus().name());
            ps.setBigDecimal(18, reservation.getAmountPaid());
            ps.setObject(19, reservation.getRefund());

            var rs = ps.getGeneratedKeys();
            if (rs.next()) {
                long id = rs.getLong(1);
                return findById(id).orElseThrow();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save reservation", e);
        }
        throw new RuntimeException("Failed to get generated id");
    }

    public Optional<Reservation> findById(long id) {
        try (Connection conn = Database.getInstance().getConnection()) {
            var ps = conn.prepareStatement(
                """
                SELECT id, room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name,
                       guest_passport, guest_country, check_in, check_out, guests, rate_plan, nights,
                       subtotal, discount, total, status, amount_paid, refund
                FROM reservations WHERE id = ?
                """
            );
            ps.setLong(1, id);
            var rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservation", e);
        }
        return Optional.empty();
    }

    public void update(Reservation reservation) {
        try (Connection conn = Database.getInstance().getConnection()) {
            var ps = conn.prepareStatement(
                """
                UPDATE reservations SET
                    status = ?, amount_paid = ?, refund = ?
                WHERE id = ?
                """
            );
            ps.setString(1, reservation.getStatus().name());
            ps.setBigDecimal(2, reservation.getAmountPaid());
            ps.setObject(3, reservation.getRefund());
            ps.setLong(4, reservation.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation", e);
        }
    }

    public List<Reservation> findByRoomAndDateRange(int roomNumber, LocalDate startDate, LocalDate endDate) {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getInstance().getConnection()) {
            var ps = conn.prepareStatement(
                """
                SELECT id, room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name,
                       guest_passport, guest_country, check_in, check_out, guests, rate_plan, nights,
                       subtotal, discount, total, status, amount_paid, refund
                FROM reservations
                WHERE room_number = ?
                  AND check_in < ? AND check_out > ?
                  AND status != 'CANCELLED'
                ORDER BY check_in
                """
            );
            ps.setInt(1, roomNumber);
            ps.setObject(2, endDate);
            ps.setObject(3, startDate);
            var rs = ps.executeQuery();
            while (rs.next()) {
                reservations.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservations", e);
        }
        return reservations;
    }

    public List<Reservation> findByDateAndRoom(LocalDate date, int roomNumber) {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getInstance().getConnection()) {
            var ps = conn.prepareStatement(
                """
                SELECT id, room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name,
                       guest_passport, guest_country, check_in, check_out, guests, rate_plan, nights,
                       subtotal, discount, total, status, amount_paid, refund
                FROM reservations
                WHERE room_number = ?
                  AND check_in <= ? AND check_out > ?
                  AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
                ORDER BY check_in
                """
            );
            ps.setInt(1, roomNumber);
            ps.setObject(2, date);
            ps.setObject(3, date);
            var rs = ps.executeQuery();
            while (rs.next()) {
                reservations.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservations", e);
        }
        return reservations;
    }

    public List<Reservation> findByMonthAndStatus(int year, int month) {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getInstance().getConnection()) {
            var ps = conn.prepareStatement(
                """
                SELECT id, room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name,
                       guest_passport, guest_country, check_in, check_out, guests, rate_plan, nights,
                       subtotal, discount, total, status, amount_paid, refund
                FROM reservations
                WHERE EXTRACT(YEAR FROM check_in) = ?
                  AND EXTRACT(MONTH FROM check_in) = ?
                  AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED')
                  AND (amount_paid > 0 OR status = 'CANCELLED')
                ORDER BY check_in, id
                """
            );
            ps.setInt(1, year);
            ps.setInt(2, month);
            var rs = ps.executeQuery();
            while (rs.next()) {
                reservations.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservations", e);
        }
        return reservations;
    }

    private Reservation mapRow(ResultSet rs) throws SQLException {
        return new Reservation(
            rs.getLong("id"),
            rs.getInt("room_number"),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            rs.getString("guest_cpf"),
            rs.getString("company_cnpj"),
            rs.getString("company_name"),
            rs.getString("guest_passport"),
            rs.getString("guest_country"),
            rs.getObject("check_in", LocalDate.class),
            rs.getObject("check_out", LocalDate.class),
            rs.getInt("guests"),
            RatePlan.valueOf(rs.getString("rate_plan")),
            rs.getInt("nights"),
            rs.getBigDecimal("subtotal"),
            rs.getBigDecimal("discount"),
            rs.getBigDecimal("total"),
            ReservationStatus.valueOf(rs.getString("status")),
            rs.getBigDecimal("amount_paid"),
            rs.getObject("refund", BigDecimal.class)
        );
    }
}
