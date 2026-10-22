package com.pousadamarealta.database;

import com.pousadamarealta.domain.Money;
import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAO {
    private static final ReservationDAO INSTANCE = new ReservationDAO();

    private ReservationDAO() {
    }

    public static ReservationDAO getInstance() {
        return INSTANCE;
    }

    public Reservation createReservation(Reservation reservation) throws SQLException {
        String sql = "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, " +
                "check_in, check_out, guests, rate_plan, status, nights, subtotal, discount, total, amount_paid) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, reservation.getRoomNumber());
            stmt.setString(2, reservation.getGuestName());
            stmt.setString(3, reservation.getGuestEmail());
            stmt.setString(4, reservation.getGuestCpf());
            stmt.setDate(5, Date.valueOf(reservation.getCheckIn()));
            stmt.setDate(6, Date.valueOf(reservation.getCheckOut()));
            stmt.setInt(7, reservation.getGuests());
            stmt.setString(8, reservation.getRatePlan().getCode());
            stmt.setString(9, reservation.getStatus().getCode());
            stmt.setInt(10, reservation.getNights());
            stmt.setBigDecimal(11, reservation.getSubtotal().toBigDecimal());
            stmt.setBigDecimal(12, reservation.getDiscount().toBigDecimal());
            stmt.setBigDecimal(13, reservation.getTotal().toBigDecimal());
            stmt.setBigDecimal(14, reservation.getAmountPaid().toBigDecimal());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id");
                    reservation.setId(id);
                }
            }
        }
        return reservation;
    }

    public Reservation getReservationById(long id) throws SQLException {
        String sql = "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, " +
                "guests, rate_plan, status, nights, subtotal, discount, total, amount_paid, refund " +
                "FROM reservations WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapReservation(rs);
                }
            }
        }
        return null;
    }

    public void updateReservation(Reservation reservation) throws SQLException {
        String sql = "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reservation.getStatus().getCode());
            stmt.setBigDecimal(2, reservation.getAmountPaid().toBigDecimal());
            if (reservation.getRefund() != null) {
                stmt.setBigDecimal(3, reservation.getRefund().toBigDecimal());
            } else {
                stmt.setNull(3, java.sql.Types.DECIMAL);
            }
            stmt.setLong(4, reservation.getId());
            stmt.executeUpdate();
        }
    }

    public List<Reservation> getReservationsByDateRange(LocalDate startDate, LocalDate endDate) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, " +
                "guests, rate_plan, status, nights, subtotal, discount, total, amount_paid, refund " +
                "FROM reservations WHERE check_in >= ? AND check_in < ? AND status != ? " +
                "ORDER BY check_in, id";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));
            stmt.setString(3, ReservationStatus.CANCELLED.getCode());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        }
        return reservations;
    }

    public List<Reservation> getReservationsByRoom(int roomNumber, LocalDate startDate, LocalDate endDate) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, " +
                "guests, rate_plan, status, nights, subtotal, discount, total, amount_paid, refund " +
                "FROM reservations WHERE room_number = ? AND check_in <= ? AND check_out > ? " +
                "AND status != ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roomNumber);
            stmt.setDate(2, Date.valueOf(endDate.minusDays(1)));
            stmt.setDate(3, Date.valueOf(startDate));
            stmt.setString(4, ReservationStatus.CANCELLED.getCode());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        }
        return reservations;
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        int roomNumber = rs.getInt("room_number");
        String guestName = rs.getString("guest_name");
        String guestEmail = rs.getString("guest_email");
        String guestCpf = rs.getString("guest_cpf");
        LocalDate checkIn = rs.getDate("check_in").toLocalDate();
        LocalDate checkOut = rs.getDate("check_out").toLocalDate();
        int guests = rs.getInt("guests");
        RatePlan ratePlan = RatePlan.fromCode(rs.getString("rate_plan"));
        ReservationStatus status = ReservationStatus.fromCode(rs.getString("status"));
        int nights = rs.getInt("nights");
        Money subtotal = Money.of(rs.getBigDecimal("subtotal"));
        Money discount = Money.of(rs.getBigDecimal("discount"));
        Money total = Money.of(rs.getBigDecimal("total"));
        Money amountPaid = Money.of(rs.getBigDecimal("amount_paid"));
        Money refund = rs.getBigDecimal("refund") != null ? Money.of(rs.getBigDecimal("refund")) : null;

        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan, status, nights, subtotal, discount, total,
                amountPaid, refund);
    }
}
