package br.com.marealto.repository;

import br.com.marealto.model.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {

    public Reservation save(Reservation reservation) {
        String sql = """
            INSERT INTO reservations (
                room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name,
                guest_passport, guest_country, check_in, check_out, guests, rate_plan,
                nights, subtotal, discount, total, status, amount_paid, refund
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = Database.getConnection();
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
            throw new RuntimeException("Failed to save reservation", e);
        }
    }

    public void update(Reservation reservation) {
        String sql = """
            UPDATE reservations SET
                room_number = ?, guest_name = ?, guest_email = ?, guest_cpf = ?, company_cnpj = ?,
                company_name = ?, guest_passport = ?, guest_country = ?, check_in = ?, check_out = ?,
                guests = ?, rate_plan = ?, nights = ?, subtotal = ?, discount = ?, total = ?,
                status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setReservationParameters(stmt, reservation);
            stmt.setLong(20, reservation.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation", e);
        }
    }

    private void setReservationParameters(PreparedStatement stmt, Reservation r) throws SQLException {
        stmt.setString(1, r.getRoomNumber());
        stmt.setString(2, r.getGuestName());
        stmt.setString(3, r.getGuestEmail());

        GuestIdentification id = r.getGuestIdentification();
        if (id instanceof GuestIdentification.Individual individual) {
            stmt.setString(4, individual.cpf());
            stmt.setNull(5, Types.VARCHAR);
            stmt.setNull(6, Types.VARCHAR);
            stmt.setNull(7, Types.VARCHAR);
            stmt.setNull(8, Types.VARCHAR);
        } else if (id instanceof GuestIdentification.Company company) {
            stmt.setNull(4, Types.VARCHAR);
            stmt.setString(5, company.cnpj());
            stmt.setString(6, company.companyName());
            stmt.setNull(7, Types.VARCHAR);
            stmt.setNull(8, Types.VARCHAR);
        } else if (id instanceof GuestIdentification.Foreign foreign) {
            stmt.setNull(4, Types.VARCHAR);
            stmt.setNull(5, Types.VARCHAR);
            stmt.setNull(6, Types.VARCHAR);
            stmt.setString(7, foreign.passport());
            stmt.setString(8, foreign.country());
        }

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

        if (r.getRefund() != null) {
            stmt.setBigDecimal(19, r.getRefund());
        } else {
            stmt.setNull(19, Types.DECIMAL);
        }
    }

    public Optional<Reservation> findById(Long id) {
        String sql = "SELECT * FROM reservations WHERE id = ?";

        try (Connection conn = Database.getConnection();
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

    public List<Reservation> findConflicting(String roomNumber, LocalDate checkIn, LocalDate checkOut) {
        String sql = """
            SELECT * FROM reservations
            WHERE room_number = ?
            AND status NOT IN ('CANCELLED')
            AND check_in < ?
            AND check_out > ?
        """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = Database.getConnection();
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
            throw new RuntimeException("Failed to check conflicts", e);
        }

        return reservations;
    }

    public List<Reservation> findByCheckInMonth(YearMonth month) {
        String sql = """
            SELECT * FROM reservations
            WHERE EXTRACT(YEAR FROM check_in) = ?
            AND EXTRACT(MONTH FROM check_in) = ?
            AND status != 'PENDING'
            ORDER BY check_in, id
        """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, month.getYear());
            stmt.setInt(2, month.getMonthValue());

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

    public List<Reservation> findByNight(LocalDate night) {
        String sql = """
            SELECT * FROM reservations
            WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            AND check_in <= ?
            AND check_out > ?
            ORDER BY room_number
        """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = Database.getConnection();
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

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getLong("id"));
        r.setRoomNumber(rs.getString("room_number"));
        r.setGuestName(rs.getString("guest_name"));
        r.setGuestEmail(rs.getString("guest_email"));

        String cpf = rs.getString("guest_cpf");
        String cnpj = rs.getString("company_cnpj");
        String passport = rs.getString("guest_passport");

        if (cpf != null) {
            r.setGuestIdentification(new GuestIdentification.Individual(cpf));
        } else if (cnpj != null) {
            r.setGuestIdentification(new GuestIdentification.Company(cnpj, rs.getString("company_name")));
        } else if (passport != null) {
            r.setGuestIdentification(new GuestIdentification.Foreign(passport, rs.getString("guest_country")));
        }

        r.setCheckIn(rs.getDate("check_in").toLocalDate());
        r.setCheckOut(rs.getDate("check_out").toLocalDate());
        r.setGuests(rs.getInt("guests"));
        r.setRatePlan(RatePlan.valueOf(rs.getString("rate_plan")));
        r.setNights(rs.getInt("nights"));
        r.setSubtotal(rs.getBigDecimal("subtotal"));
        r.setDiscount(rs.getBigDecimal("discount"));
        r.setTotal(rs.getBigDecimal("total"));
        r.setStatus(ReservationStatus.valueOf(rs.getString("status")));
        r.setAmountPaid(rs.getBigDecimal("amount_paid"));

        BigDecimal refund = rs.getBigDecimal("refund");
        if (!rs.wasNull()) {
            r.setRefund(refund);
        }

        return r;
    }
}
