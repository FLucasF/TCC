package br.com.marealte.repository;

import br.com.marealte.database.Database;
import br.com.marealte.model.RatePlan;
import br.com.marealte.model.Reservation;
import br.com.marealte.model.ReservationStatus;
import br.com.marealte.model.RoomType;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class ReservationRepository {

    public Reservation save(Reservation reservation) {
        String sql = """
            INSERT INTO reservations (
                room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name,
                guest_passport, guest_country, check_in, check_out, guests, rate_plan,
                nights, subtotal, discount, total, status, amount_paid
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, reservation.getRoomNumber());
            stmt.setString(2, reservation.getGuestName());
            stmt.setString(3, reservation.getGuestEmail());
            stmt.setString(4, reservation.getGuestCpf());
            stmt.setString(5, reservation.getCompanyCnpj());
            stmt.setString(6, reservation.getCompanyName());
            stmt.setString(7, reservation.getGuestPassport());
            stmt.setString(8, reservation.getGuestCountry());
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

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    reservation.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save reservation", e);
        }

        return reservation;
    }

    public Reservation findById(Long id) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name,
                   guest_passport, guest_country, check_in, check_out, guests, rate_plan,
                   nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations WHERE id = ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapReservation(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservation", e);
        }

        return null;
    }

    public void update(Reservation reservation) {
        String sql = """
            UPDATE reservations
            SET status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
        """;

        try (Connection conn = Database.getConnection();
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

    public boolean isRoomAvailable(String roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeReservationId) {
        String sql = """
            SELECT COUNT(*) FROM reservations
            WHERE room_number = ?
            AND status != 'CANCELLED'
            AND check_in < ?
            AND check_out > ?
            AND (? IS NULL OR id != ?)
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));
            if (excludeReservationId != null) {
                stmt.setLong(4, excludeReservationId);
                stmt.setLong(5, excludeReservationId);
            } else {
                stmt.setNull(4, Types.BIGINT);
                stmt.setNull(5, Types.BIGINT);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) == 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check room availability", e);
        }

        return false;
    }

    public List<Reservation> findOccupiedOnDate(LocalDate date) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name,
                   guest_passport, guest_country, check_in, check_out, guests, rate_plan,
                   nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            AND check_in <= ?
            AND check_out > ?
            ORDER BY room_number
        """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(date));
            stmt.setDate(2, Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch occupied reservations", e);
        }

        return reservations;
    }

    public List<Reservation> findByCheckInMonth(YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name,
                   guest_passport, guest_country, check_in, check_out, guests, rate_plan,
                   nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE check_in >= ? AND check_in <= ?
            ORDER BY check_in, id
        """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(start));
            stmt.setDate(2, Date.valueOf(end));

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

    public RoomType getRoomType(String roomNumber) {
        String sql = "SELECT type FROM rooms WHERE number = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomNumber);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return RoomType.valueOf(rs.getString("type"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch room type", e);
        }

        return null;
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getLong("id"));
        r.setRoomNumber(rs.getString("room_number"));
        r.setGuestName(rs.getString("guest_name"));
        r.setGuestEmail(rs.getString("guest_email"));
        r.setGuestCpf(rs.getString("guest_cpf"));
        r.setCompanyCnpj(rs.getString("company_cnpj"));
        r.setCompanyName(rs.getString("company_name"));
        r.setGuestPassport(rs.getString("guest_passport"));
        r.setGuestCountry(rs.getString("guest_country"));
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
        r.setRefund(rs.getBigDecimal("refund"));
        return r;
    }
}
