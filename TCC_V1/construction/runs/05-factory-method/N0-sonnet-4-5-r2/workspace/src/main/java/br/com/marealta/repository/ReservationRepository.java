package br.com.marealta.repository;

import br.com.marealta.model.*;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    private final DataSource dataSource;

    public ReservationRepository(DataSource dataSource) {
        this.dataSource = dataSource;
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

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setReservationParameters(stmt, reservation);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    reservation.setId(rs.getLong("id"));
                }
            }

            return reservation;
        } catch (SQLException e) {
            throw new RuntimeException("Error saving reservation", e);
        }
    }

    public Reservation update(Reservation reservation) {
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
            return reservation;
        } catch (SQLException e) {
            throw new RuntimeException("Error updating reservation", e);
        }
    }

    public Optional<Reservation> findById(Long id) {
        String sql = """
            SELECT * FROM reservations WHERE id = ?
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

    public List<Reservation> findConflictingReservations(String roomNumber, LocalDate checkIn, LocalDate checkOut) {
        String sql = """
            SELECT * FROM reservations
            WHERE room_number = ?
            AND status NOT IN ('CANCELLED')
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
            throw new RuntimeException("Error finding conflicting reservations", e);
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
        } catch (SQLException e) {
            throw new RuntimeException("Error finding reservations by night", e);
        }

        return reservations;
    }

    public List<Reservation> findByMonth(int year, int month) {
        String sql = """
            SELECT * FROM reservations
            WHERE EXTRACT(YEAR FROM check_in) = ?
            AND EXTRACT(MONTH FROM check_in) = ?
            AND (amount_paid - COALESCE(refund, 0)) > 0
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
            throw new RuntimeException("Error finding reservations by month", e);
        }

        return reservations;
    }

    private void setReservationParameters(PreparedStatement stmt, Reservation reservation) throws SQLException {
        stmt.setString(1, reservation.getRoomNumber());
        stmt.setString(2, reservation.getGuestName());
        stmt.setString(3, reservation.getGuestEmail());

        GuestIdentification id = reservation.getGuestIdentification();
        stmt.setString(4, id instanceof GuestIdentification.IndividualGuest ig ? ig.cpf() : null);
        stmt.setString(5, id instanceof GuestIdentification.CorporateGuest cg ? cg.cnpj() : null);
        stmt.setString(6, id instanceof GuestIdentification.CorporateGuest cg ? cg.companyName() : null);
        stmt.setString(7, id instanceof GuestIdentification.ForeignGuest fg ? fg.passport() : null);
        stmt.setString(8, id instanceof GuestIdentification.ForeignGuest fg ? fg.country() : null);

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
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        Reservation reservation = new Reservation();
        reservation.setId(rs.getLong("id"));
        reservation.setRoomNumber(rs.getString("room_number"));
        reservation.setGuestName(rs.getString("guest_name"));
        reservation.setGuestEmail(rs.getString("guest_email"));

        String cpf = rs.getString("guest_cpf");
        String cnpj = rs.getString("company_cnpj");
        String passport = rs.getString("guest_passport");

        if (cpf != null) {
            reservation.setGuestIdentification(new GuestIdentification.IndividualGuest(cpf));
        } else if (cnpj != null) {
            String companyName = rs.getString("company_name");
            reservation.setGuestIdentification(new GuestIdentification.CorporateGuest(cnpj, companyName));
        } else if (passport != null) {
            String country = rs.getString("guest_country");
            reservation.setGuestIdentification(new GuestIdentification.ForeignGuest(passport, country));
        }

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
        reservation.setRefund(refund);

        return reservation;
    }
}
