package com.pousada.marealta.persistence;

import com.pousada.marealta.model.RatePlan;
import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.model.ReservationStatus;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationDao {
    private final DataSource dataSource;

    public ReservationDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Reservation save(Reservation reservation) throws SQLException {
        String sql = "INSERT INTO reservations " +
                     "(room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, " +
                     "rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid, refund) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, reservation.getRoomNumber());
            stmt.setString(2, reservation.getGuestName());
            stmt.setString(3, reservation.getGuestEmail());
            stmt.setString(4, reservation.getGuestCpf());
            stmt.setDate(5, Date.valueOf(reservation.getCheckIn()));
            stmt.setDate(6, Date.valueOf(reservation.getCheckOut()));
            stmt.setInt(7, reservation.getGuests());
            stmt.setString(8, reservation.getRatePlan().name());
            stmt.setString(9, reservation.getCompanyCode());
            stmt.setInt(10, reservation.getNights());
            stmt.setBigDecimal(11, reservation.getSubtotal());
            stmt.setBigDecimal(12, reservation.getDiscount());
            stmt.setBigDecimal(13, reservation.getTotal());
            stmt.setString(14, reservation.getStatus().name());
            stmt.setBigDecimal(15, reservation.getAmountPaid());
            stmt.setObject(16, reservation.getRefund());

            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                reservation.setId(rs.getLong(1));
            }
        }

        return reservation;
    }

    public void update(Reservation reservation) throws SQLException {
        String sql = "UPDATE reservations SET " +
                     "guest_name = ?, guest_email = ?, guest_cpf = ?, check_in = ?, check_out = ?, " +
                     "guests = ?, rate_plan = ?, company_code = ?, nights = ?, subtotal = ?, " +
                     "discount = ?, total = ?, status = ?, amount_paid = ?, refund = ? " +
                     "WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reservation.getGuestName());
            stmt.setString(2, reservation.getGuestEmail());
            stmt.setString(3, reservation.getGuestCpf());
            stmt.setDate(4, Date.valueOf(reservation.getCheckIn()));
            stmt.setDate(5, Date.valueOf(reservation.getCheckOut()));
            stmt.setInt(6, reservation.getGuests());
            stmt.setString(7, reservation.getRatePlan().name());
            stmt.setString(8, reservation.getCompanyCode());
            stmt.setInt(9, reservation.getNights());
            stmt.setBigDecimal(10, reservation.getSubtotal());
            stmt.setBigDecimal(11, reservation.getDiscount());
            stmt.setBigDecimal(12, reservation.getTotal());
            stmt.setString(13, reservation.getStatus().name());
            stmt.setBigDecimal(14, reservation.getAmountPaid());
            stmt.setObject(15, reservation.getRefund());
            stmt.setLong(16, reservation.getId());

            stmt.executeUpdate();
        }
    }

    public Reservation findById(long id) throws SQLException {
        String sql = "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, " +
                     "guests, rate_plan, company_code, nights, subtotal, discount, total, status, " +
                     "amount_paid, refund FROM reservations WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapReservation(rs);
            }
            return null;
        }
    }

    public List<Reservation> findByRoomAndDateRange(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, " +
                     "guests, rate_plan, company_code, nights, subtotal, discount, total, status, " +
                     "amount_paid, refund FROM reservations " +
                     "WHERE room_number = ? AND status != 'CANCELLED' " +
                     "AND check_in < ? AND check_out > ? " +
                     "ORDER BY check_in";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapReservation(rs));
            }
        }

        return reservations;
    }

    public List<Reservation> findByMonthAndStatus(int year, int month) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, " +
                     "guests, rate_plan, company_code, nights, subtotal, discount, total, status, " +
                     "amount_paid, refund FROM reservations " +
                     "WHERE EXTRACT(YEAR FROM check_in) = ? AND EXTRACT(MONTH FROM check_in) = ? " +
                     "AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') " +
                     "ORDER BY check_in, id";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, year);
            stmt.setInt(2, month);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapReservation(rs));
            }
        }

        return reservations;
    }

    public List<Reservation> findByDateAndStatus(LocalDate date) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, " +
                     "guests, rate_plan, company_code, nights, subtotal, discount, total, status, " +
                     "amount_paid, refund FROM reservations " +
                     "WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') " +
                     "AND check_in <= ? AND check_out > ? " +
                     "ORDER BY room_number";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(date));
            stmt.setDate(2, Date.valueOf(date));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapReservation(rs));
            }
        }

        return reservations;
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getLong("id"));
        r.setRoomNumber(rs.getInt("room_number"));
        r.setGuestName(rs.getString("guest_name"));
        r.setGuestEmail(rs.getString("guest_email"));
        r.setGuestCpf(rs.getString("guest_cpf"));
        r.setCheckIn(rs.getDate("check_in").toLocalDate());
        r.setCheckOut(rs.getDate("check_out").toLocalDate());
        r.setGuests(rs.getInt("guests"));
        r.setRatePlan(RatePlan.valueOf(rs.getString("rate_plan")));
        r.setCompanyCode(rs.getString("company_code"));
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
