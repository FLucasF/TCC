package br.com.pousadamareal.repository;

import br.com.pousadamareal.db.DatabaseConnection;
import br.com.pousadamareal.domain.RatePlan;
import br.com.pousadamareal.domain.Reservation;
import br.com.pousadamareal.domain.ReservationStatus;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationRepository {
    public Reservation save(Reservation reservation) throws SQLException {
        String sql = """
            INSERT INTO reservations (
                room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name,
                guest_passport, guest_country, check_in, check_out, guests, rate_plan,
                nights, subtotal, discount, total, status, amount_paid, refund
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, reservation.getRoomNumber());
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
                stmt.setString(12, reservation.getRatePlan().getValue());
                stmt.setInt(13, reservation.getNights());
                stmt.setBigDecimal(14, java.math.BigDecimal.valueOf(reservation.getSubtotal()));
                stmt.setBigDecimal(15, java.math.BigDecimal.valueOf(reservation.getDiscount()));
                stmt.setBigDecimal(16, java.math.BigDecimal.valueOf(reservation.getTotal()));
                stmt.setString(17, reservation.getStatus().getValue());
                stmt.setBigDecimal(18, java.math.BigDecimal.valueOf(reservation.getAmountPaid()));
                stmt.setObject(19, reservation.getRefund() != null ? java.math.BigDecimal.valueOf(reservation.getRefund()) : null);

                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        reservation.setId(rs.getLong(1));
                    }
                }
            }
        }
        return reservation;
    }

    public Reservation findById(long id) throws SQLException {
        String sql = "SELECT * FROM reservations WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSet(rs);
                    }
                }
            }
        }
        return null;
    }

    public void update(Reservation reservation) throws SQLException {
        String sql = """
            UPDATE reservations SET
                status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, reservation.getStatus().getValue());
                stmt.setBigDecimal(2, java.math.BigDecimal.valueOf(reservation.getAmountPaid()));
                stmt.setObject(3, reservation.getRefund() != null ? java.math.BigDecimal.valueOf(reservation.getRefund()) : null);
                stmt.setLong(4, reservation.getId());
                stmt.executeUpdate();
            }
        }
    }

    public List<Reservation> findByRoomAndDateRange(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        String sql = """
            SELECT * FROM reservations
            WHERE room_number = ?
            AND status != 'CANCELLED'
            AND check_in < ?
            AND check_out > ?
            """;

        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, roomNumber);
                stmt.setDate(2, Date.valueOf(checkOut));
                stmt.setDate(3, Date.valueOf(checkIn));
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        reservations.add(mapResultSet(rs));
                    }
                }
            }
        }
        return reservations;
    }

    public List<Reservation> findByRoomAndNight(int roomNumber, LocalDate night) throws SQLException {
        String sql = """
            SELECT * FROM reservations
            WHERE room_number = ?
            AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            AND check_in <= ?
            AND check_out > ?
            """;

        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, roomNumber);
                stmt.setDate(2, Date.valueOf(night));
                stmt.setDate(3, Date.valueOf(night));
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        reservations.add(mapResultSet(rs));
                    }
                }
            }
        }
        return reservations;
    }

    public List<Reservation> findByMonthAndPaid(int year, int month) throws SQLException {
        String sql = """
            SELECT * FROM reservations
            WHERE EXTRACT(YEAR FROM check_in) = ?
            AND EXTRACT(MONTH FROM check_in) = ?
            AND (amount_paid > 0 OR refund IS NOT NULL)
            ORDER BY check_in, id
            """;

        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, year);
                stmt.setInt(2, month);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        reservations.add(mapResultSet(rs));
                    }
                }
            }
        }
        return reservations;
    }

    private Reservation mapResultSet(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getLong("id"));
        r.setRoomNumber(rs.getInt("room_number"));
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
        r.setSubtotal(rs.getDouble("subtotal"));
        r.setDiscount(rs.getDouble("discount"));
        r.setTotal(rs.getDouble("total"));
        r.setStatus(ReservationStatus.valueOf(rs.getString("status")));
        r.setAmountPaid(rs.getDouble("amount_paid"));
        Double refund = rs.getDouble("refund");
        r.setRefund(rs.wasNull() ? null : refund);
        return r;
    }
}
