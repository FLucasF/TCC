package com.pousadamareaita.service;

import com.pousadamareaita.database.Database;
import com.pousadamareaita.model.Reservation;
import com.pousadamareaita.model.Room;
import com.pousadamareaita.util.MoneyUtils;
import com.pousadamareaita.exception.ApiException;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ReservationService {
    private final Database db;
    private final RoomService roomService;

    public ReservationService(Database db) {
        this.db = db;
        this.roomService = new RoomService(db);
    }

    public Reservation createReservation(String guestName, String guestEmail,
                                        String guestCpf, String companyCnpj, String companyName,
                                        String guestPassport, String guestCountry,
                                        int roomNumber, LocalDate checkIn, LocalDate checkOut,
                                        int guests, String ratePlan) throws SQLException {
        validateIdentification(guestCpf, companyCnpj, companyName, guestPassport, guestCountry);

        if (checkOut.isBefore(checkIn) || checkOut.equals(checkIn)) {
            throw new ApiException("INVALID_REQUEST", "Check-out date must be after check-in date", 400);
        }

        if (guests < 1) {
            throw new ApiException("INVALID_REQUEST", "At least 1 guest is required", 400);
        }

        Room room = roomService.getRoomByNumber(roomNumber);
        if (room == null) {
            throw new ApiException("ROOM_NOT_FOUND", "Room not found", 404);
        }

        if (guests > room.getCapacity()) {
            throw new ApiException("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity", 422);
        }

        if (isRoomUnavailable(roomNumber, checkIn, checkOut)) {
            throw new ApiException("ROOM_UNAVAILABLE", "Room is not available for selected dates", 409);
        }

        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);

        BigDecimal subtotal = calculateSubtotal(room.getNightlyRate(), (int) nights, checkIn, checkOut);
        BigDecimal discount = calculateDiscount(subtotal, ratePlan);
        BigDecimal total = MoneyUtils.round(subtotal.subtract(discount));

        try (Connection conn = db.getConnection()) {
            String sql = """
                INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, company_cnpj,
                                        company_name, guest_passport, guest_country, check_in, check_out,
                                        guests, rate_plan, nights, subtotal, discount, total, status, amount_paid)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', 0)
            """;

            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, roomNumber);
                pstmt.setString(2, guestName);
                pstmt.setString(3, guestEmail);
                pstmt.setString(4, guestCpf);
                pstmt.setString(5, companyCnpj);
                pstmt.setString(6, companyName);
                pstmt.setString(7, guestPassport);
                pstmt.setString(8, guestCountry);
                pstmt.setDate(9, java.sql.Date.valueOf(checkIn));
                pstmt.setDate(10, java.sql.Date.valueOf(checkOut));
                pstmt.setInt(11, guests);
                pstmt.setString(12, ratePlan);
                pstmt.setInt(13, (int) nights);
                pstmt.setBigDecimal(14, subtotal);
                pstmt.setBigDecimal(15, discount);
                pstmt.setBigDecimal(16, total);

                pstmt.executeUpdate();

                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        long reservationId = generatedKeys.getLong(1);
                        return getReservation(reservationId);
                    }
                }
            }
        }

        return null;
    }

    public Reservation getReservation(long id) throws SQLException {
        try (Connection conn = db.getConnection()) {
            String sql = """
                SELECT id, room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name,
                       guest_passport, guest_country, check_in, check_out, guests, rate_plan, nights,
                       subtotal, discount, total, status, amount_paid, refund
                FROM reservations WHERE id = ?
            """;

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, id);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return mapReservation(rs);
                    }
                }
            }
        }
        return null;
    }

    public void confirmReservation(long id, BigDecimal amountPaid) throws SQLException {
        Reservation res = getReservation(id);
        if (res == null) {
            throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found", 404);
        }

        if (!res.getStatus().equals("PENDING")) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Can only confirm PENDING reservations", 409);
        }

        try (Connection conn = db.getConnection()) {
            String sql = "UPDATE reservations SET status = 'CONFIRMED', amount_paid = ? WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setBigDecimal(1, amountPaid);
                pstmt.setLong(2, id);
                pstmt.executeUpdate();
            }
        }
    }

    public void checkInReservation(long id) throws SQLException {
        Reservation res = getReservation(id);
        if (res == null) {
            throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found", 404);
        }

        if (!res.getStatus().equals("CONFIRMED")) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Can only check in CONFIRMED reservations", 409);
        }

        try (Connection conn = db.getConnection()) {
            String sql = "UPDATE reservations SET status = 'CHECKED_IN' WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, id);
                pstmt.executeUpdate();
            }
        }
    }

    public void checkOutReservation(long id) throws SQLException {
        Reservation res = getReservation(id);
        if (res == null) {
            throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found", 404);
        }

        if (!res.getStatus().equals("CHECKED_IN")) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Can only check out CHECKED_IN reservations", 409);
        }

        try (Connection conn = db.getConnection()) {
            String sql = "UPDATE reservations SET status = 'CHECKED_OUT' WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, id);
                pstmt.executeUpdate();
            }
        }
    }

    public void cancelReservation(long id) throws SQLException {
        Reservation res = getReservation(id);
        if (res == null) {
            throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found", 404);
        }

        if (!res.getStatus().equals("PENDING") && !res.getStatus().equals("CONFIRMED")) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Can only cancel PENDING or CONFIRMED reservations", 409);
        }

        BigDecimal refund = BigDecimal.ZERO;
        if (res.getStatus().equals("CONFIRMED") && res.getRatePlan().equals("STANDARD")) {
            refund = res.getAmountPaid();
        }

        try (Connection conn = db.getConnection()) {
            String sql = "UPDATE reservations SET status = 'CANCELLED', refund = ? WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setBigDecimal(1, refund);
                pstmt.setLong(2, id);
                pstmt.executeUpdate();
            }
        }
    }

    public List<Reservation> getReservationsByRoomAndDate(int roomNumber, LocalDate date) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = db.getConnection()) {
            String sql = """
                SELECT id, room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name,
                       guest_passport, guest_country, check_in, check_out, guests, rate_plan, nights,
                       subtotal, discount, total, status, amount_paid, refund
                FROM reservations
                WHERE room_number = ? AND check_in <= ? AND check_out > ? AND status != 'CANCELLED'
                ORDER BY check_in
            """;

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, roomNumber);
                pstmt.setDate(2, java.sql.Date.valueOf(date));
                pstmt.setDate(3, java.sql.Date.valueOf(date));

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        reservations.add(mapReservation(rs));
                    }
                }
            }
        }
        return reservations;
    }

    public List<Reservation> getReservationsByMonthWithPayment(YearMonth yearMonth) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = db.getConnection()) {
            String sql = """
                SELECT id, room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name,
                       guest_passport, guest_country, check_in, check_out, guests, rate_plan, nights,
                       subtotal, discount, total, status, amount_paid, refund
                FROM reservations
                WHERE EXTRACT(YEAR FROM check_in) = ? AND EXTRACT(MONTH FROM check_in) = ?
                  AND (status = 'CONFIRMED' OR status = 'CHECKED_IN' OR status = 'CHECKED_OUT' OR (status = 'CANCELLED' AND amount_paid > 0))
                ORDER BY check_in, id
            """;

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, yearMonth.getYear());
                pstmt.setInt(2, yearMonth.getMonthValue());

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        reservations.add(mapReservation(rs));
                    }
                }
            }
        }
        return reservations;
    }

    private boolean isRoomUnavailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        try (Connection conn = db.getConnection()) {
            String sql = """
                SELECT COUNT(*) FROM reservations
                WHERE room_number = ? AND check_in < ? AND check_out > ? AND status != 'CANCELLED'
            """;

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, roomNumber);
                pstmt.setDate(2, java.sql.Date.valueOf(checkOut));
                pstmt.setDate(3, java.sql.Date.valueOf(checkIn));

                try (ResultSet rs = pstmt.executeQuery()) {
                    rs.next();
                    return rs.getInt(1) > 0;
                }
            }
        }
    }

    private BigDecimal calculateSubtotal(BigDecimal nightlyRate, int nights, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal total = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal rate = nightlyRate;

            if (isHighSeason(current)) {
                rate = rate.multiply(new BigDecimal("1.20"));
            }

            rate = MoneyUtils.round(rate);
            total = total.add(rate);
            current = current.plusDays(1);
        }

        return MoneyUtils.round(total);
    }

    private BigDecimal calculateDiscount(BigDecimal subtotal, String ratePlan) {
        if ("NON_REFUNDABLE".equals(ratePlan)) {
            BigDecimal discount = subtotal.multiply(new BigDecimal("0.15"));
            return MoneyUtils.round(discount);
        }
        return BigDecimal.ZERO;
    }

    private boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) return true;
        if (month == 12) return true;
        if (month == 1) return true;
        if (month == 2) return day <= 28;

        return false;
    }

    private void validateIdentification(String guestCpf, String companyCnpj, String companyName,
                                       String guestPassport, String guestCountry) {
        int identificationCount = 0;

        if (guestCpf != null && !guestCpf.isEmpty()) identificationCount++;
        if (companyCnpj != null && !companyCnpj.isEmpty()) identificationCount++;
        if (guestPassport != null && !guestPassport.isEmpty()) identificationCount++;

        if (identificationCount != 1) {
            throw new ApiException("INVALID_REQUEST", "Exactly one identification method must be provided", 400);
        }

        if (companyCnpj != null && !companyCnpj.isEmpty() &&
            (companyName == null || companyName.isEmpty())) {
            throw new ApiException("INVALID_REQUEST", "Company name is required when using CNPJ", 400);
        }

        if (guestPassport != null && !guestPassport.isEmpty() &&
            (guestCountry == null || guestCountry.isEmpty())) {
            throw new ApiException("INVALID_REQUEST", "Guest country is required when using passport", 400);
        }
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
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
            rs.getDate("check_in").toLocalDate(),
            rs.getDate("check_out").toLocalDate(),
            rs.getInt("guests"),
            rs.getString("rate_plan"),
            rs.getInt("nights"),
            rs.getBigDecimal("subtotal"),
            rs.getBigDecimal("discount"),
            rs.getBigDecimal("total"),
            rs.getString("status"),
            rs.getBigDecimal("amount_paid"),
            rs.getBigDecimal("refund")
        );
    }
}
