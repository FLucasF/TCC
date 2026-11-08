package com.pousada.service;

import com.pousada.database.Database;
import com.pousada.domain.*;
import com.pousada.util.MoneyRounder;
import com.pousada.util.SeasonUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.*;

public class ReservationService {
    private static final Map<String, Integer> COMPANY_DISCOUNTS = Map.of(
        "TECNOSUL", 12,
        "AGROVALE", 18,
        "FARMAVIDA", 25
    );

    private static final Map<String, BigDecimal> WEEKEND_PACKAGE_PRICES = Map.of(
        "STANDARD", new BigDecimal("520.00"),
        "DELUXE", new BigDecimal("820.00"),
        "SUITE", new BigDecimal("1200.00")
    );

    public long createReservation(int roomNumber, String guestName, String guestEmail,
                                  String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                  int guests, RatePlan ratePlan, String companyCode) throws Exception {
        Room room = RoomService.getRoomByNumber(roomNumber);
        if (room == null) {
            throw new IllegalArgumentException("ROOM_NOT_FOUND");
        }

        if (guests < 1 || guests > room.getCapacity()) {
            throw new IllegalArgumentException("CAPACITY_EXCEEDED");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        validateRatePlanRules(ratePlan, checkIn, checkOut, companyCode);

        if (isRoomUnavailable(roomNumber, checkIn, checkOut)) {
            throw new IllegalArgumentException("ROOM_UNAVAILABLE");
        }

        int nights = calculateNights(checkIn, checkOut);
        PricingResult pricing = calculatePrice(room, checkIn, checkOut, nights, ratePlan, companyCode);

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid, refund) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, roomNumber);
            stmt.setString(2, guestName);
            stmt.setString(3, guestEmail);
            stmt.setString(4, guestCpf);
            stmt.setDate(5, Date.valueOf(checkIn));
            stmt.setDate(6, Date.valueOf(checkOut));
            stmt.setInt(7, guests);
            stmt.setString(8, ratePlan.toString());
            stmt.setString(9, companyCode);
            stmt.setInt(10, nights);
            stmt.setBigDecimal(11, pricing.subtotal);
            stmt.setBigDecimal(12, pricing.discount);
            stmt.setBigDecimal(13, pricing.total);
            stmt.setString(14, ReservationStatus.PENDING.toString());
            stmt.setBigDecimal(15, BigDecimal.ZERO);
            stmt.setNull(16, Types.NUMERIC);

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }

        throw new RuntimeException("Failed to create reservation");
    }

    public Reservation getReservation(long id) throws Exception {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid, refund FROM reservations WHERE id = ?")) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapReservation(rs);
                }
            }
        }
        return null;
    }

    public void confirmReservation(long id, BigDecimal amountPaid) throws Exception {
        Reservation res = getReservation(id);
        if (res == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (res.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "UPDATE reservations SET status = ?, amount_paid = ? WHERE id = ?")) {

            stmt.setString(1, ReservationStatus.CONFIRMED.toString());
            stmt.setBigDecimal(2, amountPaid);
            stmt.setLong(3, id);
            stmt.executeUpdate();
        }
    }

    public void checkIn(long id) throws Exception {
        Reservation res = getReservation(id);
        if (res == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (res.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "UPDATE reservations SET status = ? WHERE id = ?")) {

            stmt.setString(1, ReservationStatus.CHECKED_IN.toString());
            stmt.setLong(2, id);
            stmt.executeUpdate();
        }
    }

    public void checkOut(long id) throws Exception {
        Reservation res = getReservation(id);
        if (res == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (res.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "UPDATE reservations SET status = ? WHERE id = ?")) {

            stmt.setString(1, ReservationStatus.CHECKED_OUT.toString());
            stmt.setLong(2, id);
            stmt.executeUpdate();
        }
    }

    public void cancelReservation(long id) throws Exception {
        Reservation res = getReservation(id);
        if (res == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (res.getStatus() != ReservationStatus.PENDING && res.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        BigDecimal refund = calculateRefund(res);

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "UPDATE reservations SET status = ?, refund = ? WHERE id = ?")) {

            stmt.setString(1, ReservationStatus.CANCELLED.toString());
            if (refund != null) {
                stmt.setBigDecimal(2, refund);
            } else {
                stmt.setNull(2, Types.NUMERIC);
            }
            stmt.setLong(3, id);
            stmt.executeUpdate();
        }
    }

    private BigDecimal calculateRefund(Reservation res) {
        if (res.getStatus() == ReservationStatus.PENDING) {
            return null;
        }

        return switch (res.getRatePlan()) {
            case STANDARD -> res.getAmountPaid();
            case NON_REFUNDABLE -> BigDecimal.ZERO;
            case CORPORATE -> res.getAmountPaid();
            case WEEKEND_PACKAGE -> MoneyRounder.round(res.getAmountPaid().multiply(new BigDecimal("0.5")));
            case LONG_STAY -> res.getAmountPaid();
        };
    }

    private void validateRatePlanRules(RatePlan ratePlan, LocalDate checkIn, LocalDate checkOut, String companyCode) throws Exception {
        switch (ratePlan) {
            case CORPORATE:
                if (companyCode == null || !COMPANY_DISCOUNTS.containsKey(companyCode)) {
                    throw new IllegalArgumentException("INVALID_REQUEST");
                }
                break;
            case WEEKEND_PACKAGE:
                if (!isWeekendPackageValid(checkIn, checkOut)) {
                    throw new IllegalArgumentException("INVALID_REQUEST");
                }
                break;
            case LONG_STAY:
                if (calculateNights(checkIn, checkOut) < 7) {
                    throw new IllegalArgumentException("INVALID_REQUEST");
                }
                break;
        }
    }

    private boolean isWeekendPackageValid(LocalDate checkIn, LocalDate checkOut) {
        if (calculateNights(checkIn, checkOut) != 2) {
            return false;
        }

        int dayOfWeek = checkIn.getDayOfWeek().getValue();
        int dayOfWeekCheckOut = checkOut.getDayOfWeek().getValue();

        return dayOfWeek == 5 && dayOfWeekCheckOut == 7;
    }

    private boolean isRoomUnavailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws Exception {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM reservations WHERE room_number = ? AND status != 'CANCELLED' AND check_in < ? AND check_out > ?")) {

            stmt.setInt(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));

            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    private int calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    private PricingResult calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut,
                                        int nights, RatePlan ratePlan, String companyCode) {
        return switch (ratePlan) {
            case STANDARD -> calculateStandardPrice(room, checkIn, nights);
            case NON_REFUNDABLE -> calculateNonRefundablePrice(room, checkIn, nights);
            case CORPORATE -> calculateCorporatePrice(room, checkIn, nights, companyCode);
            case WEEKEND_PACKAGE -> calculateWeekendPrice(room);
            case LONG_STAY -> calculateLongStayPrice(room, checkIn, nights);
        };
    }

    private PricingResult calculateStandardPrice(Room room, LocalDate checkIn, int nights) {
        BigDecimal subtotal = BigDecimal.ZERO;

        for (int i = 0; i < nights; i++) {
            LocalDate nightDate = checkIn.plusDays(i);
            BigDecimal rate = room.getNightlyRate();
            if (SeasonUtil.isHighSeason(nightDate)) {
                rate = MoneyRounder.round(rate.multiply(new BigDecimal("1.20")));
            }
            subtotal = subtotal.add(rate);
        }

        subtotal = MoneyRounder.round(subtotal);
        return new PricingResult(subtotal, BigDecimal.ZERO, subtotal);
    }

    private PricingResult calculateNonRefundablePrice(Room room, LocalDate checkIn, int nights) {
        PricingResult standard = calculateStandardPrice(room, checkIn, nights);
        BigDecimal discount = MoneyRounder.round(standard.subtotal.multiply(new BigDecimal("0.15")));
        BigDecimal total = MoneyRounder.round(standard.subtotal.subtract(discount));

        return new PricingResult(standard.subtotal, discount, total);
    }

    private PricingResult calculateCorporatePrice(Room room, LocalDate checkIn, int nights, String companyCode) {
        PricingResult standard = calculateStandardPrice(room, checkIn, nights);
        int discountPercent = COMPANY_DISCOUNTS.get(companyCode);
        BigDecimal discount = MoneyRounder.round(standard.subtotal.multiply(new BigDecimal(discountPercent).divide(new BigDecimal("100"), 10, java.math.RoundingMode.HALF_EVEN)));
        BigDecimal total = MoneyRounder.round(standard.subtotal.subtract(discount));

        return new PricingResult(standard.subtotal, discount, total);
    }

    private PricingResult calculateWeekendPrice(Room room) {
        BigDecimal price = WEEKEND_PACKAGE_PRICES.get(room.getType().toString());
        return new PricingResult(price, BigDecimal.ZERO, price);
    }

    private PricingResult calculateLongStayPrice(Room room, LocalDate checkIn, int nights) {
        BigDecimal subtotal = BigDecimal.ZERO;

        for (int i = 0; i < nights; i++) {
            LocalDate nightDate = checkIn.plusDays(i);
            BigDecimal rate = room.getNightlyRate();
            if (SeasonUtil.isHighSeason(nightDate)) {
                rate = MoneyRounder.round(rate.multiply(new BigDecimal("1.20")));
            }
            subtotal = subtotal.add(rate);
        }

        subtotal = MoneyRounder.round(subtotal);
        BigDecimal discount = MoneyRounder.round(subtotal.multiply(new BigDecimal("0.10")));
        BigDecimal total = MoneyRounder.round(subtotal.subtract(discount));

        return new PricingResult(subtotal, discount, total);
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
        RatePlan ratePlan = RatePlan.valueOf(rs.getString("rate_plan"));
        String companyCode = rs.getString("company_code");
        int nights = rs.getInt("nights");
        BigDecimal subtotal = rs.getBigDecimal("subtotal");
        BigDecimal discount = rs.getBigDecimal("discount");
        BigDecimal total = rs.getBigDecimal("total");
        ReservationStatus status = ReservationStatus.valueOf(rs.getString("status"));
        BigDecimal amountPaid = rs.getBigDecimal("amount_paid");
        BigDecimal refund = rs.getBigDecimal("refund");

        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut,
                              guests, ratePlan, companyCode, nights, subtotal, discount, total,
                              status, amountPaid, refund);
    }

    private static class PricingResult {
        BigDecimal subtotal;
        BigDecimal discount;
        BigDecimal total;

        PricingResult(BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
        }
    }
}
