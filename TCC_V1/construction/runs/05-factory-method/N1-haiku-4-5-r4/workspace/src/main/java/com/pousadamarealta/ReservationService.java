package com.pousadamarealta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationService {
    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);
    private final Database database;

    public ReservationService(Database database) {
        this.database = database;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                        String guestCpf, String companyCnpj, String companyName,
                                        String guestPassport, String guestCountry,
                                        LocalDate checkIn, LocalDate checkOut, int guests,
                                        RatePlan ratePlan) throws SQLException {

        RoomType roomType = getRoomType(roomNumber);
        if (roomType == null) {
            throw new ApiException("ROOM_NOT_FOUND", "Room not found", 404);
        }

        if (guests < 1 || guests > roomType.capacity) {
            throw new ApiException("CAPACITY_EXCEEDED", "Guest count exceeds room capacity", 422);
        }

        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new ApiException("INVALID_REQUEST", "Check-out must be after check-in", 400);
        }

        if (isRoomBooked(roomNumber, checkIn, checkOut)) {
            throw new ApiException("ROOM_UNAVAILABLE", "Room is not available for selected dates", 409);
        }

        int nights = PricingService.getNights(checkIn, checkOut);
        double subtotal = PricingService.calculateSubtotal(roomType, checkIn, checkOut);
        double discount = PricingService.calculateDiscount(ratePlan, subtotal);
        double total = PricingService.calculateTotal(subtotal, discount);

        try (Connection conn = database.getConnection()) {
            var stmt = conn.prepareStatement("""
                INSERT INTO reservations (room_number, guest_name, guest_email,
                    guest_cpf, company_cnpj, company_name, guest_passport, guest_country,
                    check_in, check_out, guests, rate_plan, nights, subtotal, discount,
                    total, status, amount_paid)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, java.sql.Statement.RETURN_GENERATED_KEYS);

            stmt.setInt(1, roomNumber);
            stmt.setString(2, guestName);
            stmt.setString(3, guestEmail);
            stmt.setString(4, guestCpf);
            stmt.setString(5, companyCnpj);
            stmt.setString(6, companyName);
            stmt.setString(7, guestPassport);
            stmt.setString(8, guestCountry);
            stmt.setDate(9, java.sql.Date.valueOf(checkIn));
            stmt.setDate(10, java.sql.Date.valueOf(checkOut));
            stmt.setInt(11, guests);
            stmt.setString(12, ratePlan.value);
            stmt.setInt(13, nights);
            stmt.setBigDecimal(14, new java.math.BigDecimal(subtotal));
            stmt.setBigDecimal(15, new java.math.BigDecimal(discount));
            stmt.setBigDecimal(16, new java.math.BigDecimal(total));
            stmt.setString(17, ReservationStatus.PENDING.value);
            stmt.setBigDecimal(18, new java.math.BigDecimal(0));

            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                return getReservationById(id);
            }
        }

        throw new RuntimeException("Failed to create reservation");
    }

    public Reservation getReservationById(int id) throws SQLException {
        try (Connection conn = database.getConnection()) {
            var stmt = conn.prepareStatement(
                "SELECT * FROM reservations WHERE id = ?");
            stmt.setInt(1, id);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapReservation(rs);
            }
        }

        throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found", 404);
    }

    public Reservation confirmReservation(int id, double amountPaid) throws SQLException {
        Reservation reservation = getReservationById(id);

        if (reservation.status != ReservationStatus.PENDING) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Can only confirm pending reservations", 409);
        }

        try (Connection conn = database.getConnection()) {
            var stmt = conn.prepareStatement(
                "UPDATE reservations SET status = ?, amount_paid = ? WHERE id = ?");
            stmt.setString(1, ReservationStatus.CONFIRMED.value);
            stmt.setBigDecimal(2, new java.math.BigDecimal(amountPaid));
            stmt.setInt(3, id);
            stmt.executeUpdate();

            sendConfirmationEmail(reservation, amountPaid);
        }

        return getReservationById(id);
    }

    public Reservation cancelReservation(int id) throws SQLException {
        Reservation reservation = getReservationById(id);

        if (reservation.status == ReservationStatus.CHECKED_OUT ||
            reservation.status == ReservationStatus.CANCELLED) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Cannot cancel this reservation", 409);
        }

        double refund = 0;
        if (reservation.status == ReservationStatus.CONFIRMED) {
            if (reservation.ratePlan == RatePlan.STANDARD) {
                refund = reservation.amountPaid;
            }
        }

        try (Connection conn = database.getConnection()) {
            var stmt = conn.prepareStatement(
                "UPDATE reservations SET status = ?, refund = ? WHERE id = ?");
            stmt.setString(1, ReservationStatus.CANCELLED.value);
            stmt.setBigDecimal(2, refund > 0 ? new java.math.BigDecimal(refund) : null);
            stmt.setInt(3, id);
            stmt.executeUpdate();
        }

        return getReservationById(id);
    }

    public Reservation checkIn(int id) throws SQLException {
        Reservation reservation = getReservationById(id);

        if (reservation.status != ReservationStatus.CONFIRMED) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Can only check in confirmed reservations", 409);
        }

        try (Connection conn = database.getConnection()) {
            var stmt = conn.prepareStatement(
                "UPDATE reservations SET status = ? WHERE id = ?");
            stmt.setString(1, ReservationStatus.CHECKED_IN.value);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }

        return getReservationById(id);
    }

    public Reservation checkOut(int id) throws SQLException {
        Reservation reservation = getReservationById(id);

        if (reservation.status != ReservationStatus.CHECKED_IN) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Can only check out checked in reservations", 409);
        }

        try (Connection conn = database.getConnection()) {
            var stmt = conn.prepareStatement(
                "UPDATE reservations SET status = ? WHERE id = ?");
            stmt.setString(1, ReservationStatus.CHECKED_OUT.value);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }

        return getReservationById(id);
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        return new Reservation(
            rs.getInt("id"),
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
            RatePlan.fromString(rs.getString("rate_plan")),
            rs.getInt("nights"),
            rs.getDouble("subtotal"),
            rs.getDouble("discount"),
            rs.getDouble("total"),
            ReservationStatus.fromString(rs.getString("status")),
            rs.getDouble("amount_paid"),
            rs.getObject("refund") != null ? rs.getDouble("refund") : null
        );
    }

    private RoomType getRoomType(int roomNumber) {
        return switch (roomNumber) {
            case 101, 102 -> RoomType.STANDARD;
            case 201, 202 -> RoomType.DELUXE;
            case 301 -> RoomType.SUITE;
            default -> null;
        };
    }

    private boolean isRoomBooked(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        try (Connection conn = database.getConnection()) {
            var stmt = conn.prepareStatement("""
                SELECT COUNT(*) FROM reservations
                WHERE room_number = ?
                  AND status != ?
                  AND check_in < ?
                  AND check_out > ?
                """);
            stmt.setInt(1, roomNumber);
            stmt.setString(2, ReservationStatus.CANCELLED.value);
            stmt.setDate(3, java.sql.Date.valueOf(checkOut));
            stmt.setDate(4, java.sql.Date.valueOf(checkIn));

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    private void sendConfirmationEmail(Reservation reservation, double amountPaid) throws SQLException {
        RoomType roomType = getRoomType(reservation.roomNumber);
        Document document = DocumentService.generateDocument(reservation, roomType);

        String body = "Olá, " + reservation.guestName + "!\n\n" +
                     "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
                     document.text + "\n\n" +
                     "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
                     "Até breve!";

        saveMessage("EMAIL", reservation.guestEmail, "Reserva confirmada - Pousada Maré Alta", body);
    }

    private void saveMessage(String channel, String to, String subject, String body) throws SQLException {
        try (Connection conn = database.getConnection()) {
            var stmt = conn.prepareStatement(
                "INSERT INTO messages (channel, to_address, subject, body) VALUES (?, ?, ?, ?)");
            stmt.setString(1, channel);
            stmt.setString(2, to);
            stmt.setString(3, subject);
            stmt.setString(4, body);
            stmt.executeUpdate();
        }
    }
}
