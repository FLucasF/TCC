package com.pousada.marealta.repository;

import com.pousada.marealta.config.DatabaseConfig;
import com.pousada.marealta.model.Reservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

public class ReservationRepository {
    private static final Logger logger = LoggerFactory.getLogger(ReservationRepository.class);

    public static Reservation create(Reservation reservation) throws SQLException {
        String sql = "INSERT INTO reservations " +
            "(room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name, " +
            "guest_passport, guest_country, check_in, check_out, guests, rate_plan, nights, " +
            "subtotal, discount, total, status, amount_paid) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, reservation.getRoomNumber());
            stmt.setString(2, reservation.getGuestName());
            stmt.setString(3, reservation.getGuestEmail());
            stmt.setString(4, reservation.getGuestCpf());
            stmt.setString(5, reservation.getCompanyCnpj());
            stmt.setString(6, reservation.getCompanyName());
            stmt.setString(7, reservation.getGuestPassport());
            stmt.setString(8, reservation.getGuestCountry());
            stmt.setObject(9, reservation.getCheckIn());
            stmt.setObject(10, reservation.getCheckOut());
            stmt.setInt(11, reservation.getGuests());
            stmt.setString(12, reservation.getRatePlan().name());
            stmt.setInt(13, reservation.getNights());
            stmt.setBigDecimal(14, reservation.getSubtotal());
            stmt.setBigDecimal(15, reservation.getDiscount());
            stmt.setBigDecimal(16, reservation.getTotal());
            stmt.setString(17, reservation.getStatus().name());
            stmt.setBigDecimal(18, reservation.getAmountPaid());

            stmt.executeUpdate();
            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                reservation.setId(generatedKeys.getLong(1));
            }
        }
        return reservation;
    }

    public static Reservation findById(long id) throws SQLException {
        String sql = "SELECT id, room_number, guest_name, guest_email, guest_cpf, company_cnpj, " +
            "company_name, guest_passport, guest_country, check_in, check_out, guests, rate_plan, " +
            "nights, subtotal, discount, total, status, amount_paid, refund FROM reservations WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSet(rs);
            }
        }
        return null;
    }

    public static void update(Reservation reservation) throws SQLException {
        String sql = "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, reservation.getStatus().name());
            stmt.setBigDecimal(2, reservation.getAmountPaid());
            stmt.setBigDecimal(3, reservation.getRefund());
            stmt.setLong(4, reservation.getId());
            stmt.executeUpdate();
        }
    }

    public static boolean isRoomAvailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reservations " +
            "WHERE room_number = ? " +
            "AND status != 'CANCELLED' " +
            "AND check_in < ? " +
            "AND check_out > ?";

        try (Connection conn = DatabaseConfig.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, roomNumber);
            stmt.setObject(2, checkOut);
            stmt.setObject(3, checkIn);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) == 0;
            }
        }
        return true;
    }

    private static Reservation mapResultSet(ResultSet rs) throws SQLException {
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
        r.setCheckIn(rs.getObject("check_in", LocalDate.class));
        r.setCheckOut(rs.getObject("check_out", LocalDate.class));
        r.setGuests(rs.getInt("guests"));
        r.setRatePlan(Reservation.RatePlan.valueOf(rs.getString("rate_plan")));
        r.setNights(rs.getInt("nights"));
        r.setSubtotal(rs.getBigDecimal("subtotal"));
        r.setDiscount(rs.getBigDecimal("discount"));
        r.setTotal(rs.getBigDecimal("total"));
        r.setStatus(Reservation.Status.valueOf(rs.getString("status")));
        r.setAmountPaid(rs.getBigDecimal("amount_paid"));
        r.setRefund(rs.getBigDecimal("refund"));
        return r;
    }
}
