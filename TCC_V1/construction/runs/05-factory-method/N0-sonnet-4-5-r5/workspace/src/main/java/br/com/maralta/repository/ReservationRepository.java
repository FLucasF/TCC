package br.com.maralta.repository;

import br.com.maralta.model.*;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {

    private final DataSource dataSource;

    public ReservationRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

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
            RETURNING id
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setReservationParameters(stmt, reservation);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    reservation.setId(rs.getLong("id"));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to insert reservation", e);
        }

        return reservation;
    }

    private Reservation update(Reservation reservation) {
        String sql = """
            UPDATE reservations SET
                room_number = ?, guest_name = ?, guest_email = ?, guest_cpf = ?, company_cnpj = ?,
                company_name = ?, guest_passport = ?, guest_country = ?, check_in = ?,
                check_out = ?, guests = ?, rate_plan = ?, nights = ?, subtotal = ?,
                discount = ?, total = ?, status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setReservationParameters(stmt, reservation);
            stmt.setLong(20, reservation.getId());
            stmt.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("Failed to update reservation", e);
        }

        return reservation;
    }

    private void setReservationParameters(PreparedStatement stmt, Reservation r) throws SQLException {
        GuestIdentification id = r.getIdentification();

        stmt.setString(1, r.getRoomNumber());
        stmt.setString(2, r.getGuestName());
        stmt.setString(3, r.getGuestEmail());
        stmt.setString(4, id != null ? id.getGuestCpf() : null);
        stmt.setString(5, id != null ? id.getCompanyCnpj() : null);
        stmt.setString(6, id != null ? id.getCompanyName() : null);
        stmt.setString(7, id != null ? id.getGuestPassport() : null);
        stmt.setString(8, id != null ? id.getGuestCountry() : null);
        stmt.setDate(9, Date.valueOf(r.getCheckIn()));
        stmt.setDate(10, Date.valueOf(r.getCheckOut()));
        stmt.setInt(11, r.getGuests());
        stmt.setString(12, r.getRatePlan().name());
        stmt.setInt(13, r.getNights());
        stmt.setBigDecimal(14, r.getSubtotal());
        stmt.setBigDecimal(15, r.getDiscount());
        stmt.setBigDecimal(16, r.getTotal());
        stmt.setString(17, r.getStatus().name());
        stmt.setBigDecimal(18, r.getAmountPaid());
        stmt.setBigDecimal(19, r.getRefund());
    }

    public Optional<Reservation> findById(Long id) {
        String sql = "SELECT * FROM reservations WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapReservation(rs));
                }
            }

        } catch (Exception e) {
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
            AND (? IS NULL OR id != ?)
        """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));
            stmt.setObject(4, excludeId);
            stmt.setObject(5, excludeId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to check room availability", e);
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

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(night));
            stmt.setDate(2, Date.valueOf(night));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch reservations by night", e);
        }

        return reservations;
    }

    public List<Reservation> findByMonth(YearMonth month) {
        String sql = """
            SELECT * FROM reservations
            WHERE EXTRACT(YEAR FROM check_in) = ?
            AND EXTRACT(MONTH FROM check_in) = ?
            AND (amount_paid > 0 OR refund > 0)
            ORDER BY check_in, id
        """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, month.getYear());
            stmt.setInt(2, month.getMonthValue());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch reservations by month", e);
        }

        return reservations;
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        Reservation reservation = new Reservation();
        reservation.setId(rs.getLong("id"));
        reservation.setRoomNumber(rs.getString("room_number"));
        reservation.setGuestName(rs.getString("guest_name"));
        reservation.setGuestEmail(rs.getString("guest_email"));

        GuestIdentification identification = new GuestIdentification();
        identification.setGuestCpf(rs.getString("guest_cpf"));
        identification.setCompanyCnpj(rs.getString("company_cnpj"));
        identification.setCompanyName(rs.getString("company_name"));
        identification.setGuestPassport(rs.getString("guest_passport"));
        identification.setGuestCountry(rs.getString("guest_country"));
        reservation.setIdentification(identification);

        reservation.setCheckIn(rs.getDate("check_in").toLocalDate());
        reservation.setCheckOut(rs.getDate("check_out").toLocalDate());
        reservation.setGuests(rs.getInt("guests"));
        reservation.setRatePlan(RatePlan.valueOf(rs.getString("rate_plan")));
        reservation.setNights(rs.getInt("nights"));
        reservation.setSubtotal(rs.getBigDecimal("subtotal"));
        reservation.setDiscount(rs.getBigDecimal("discount"));
        reservation.setTotal(rs.getBigDecimal("total"));
        reservation.setStatus(ReservationStatus.valueOf(rs.getString("status")));
        reservation.setAmountPaid(rs.getBigDecimal("amount_paid"));

        BigDecimal refund = rs.getBigDecimal("refund");
        reservation.setRefund(rs.wasNull() ? null : refund);

        return reservation;
    }
}
