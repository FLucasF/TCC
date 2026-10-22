package com.pousadamarealta;

import com.pousadamarealta.models.*;
import com.pousadamarealta.exceptions.*;
import com.pousadamarealta.api.*;
import com.pousadamarealta.utils.*;
import com.pousadamarealta.notifications.NotificationManager;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationService {
    public static List<Room> getRooms() {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = Database.getConnection()) {
            String sql = "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number";
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        rooms.add(new Room(
                            rs.getInt("number"),
                            RoomType.valueOf(rs.getString("type")),
                            rs.getInt("capacity"),
                            rs.getBigDecimal("nightly_rate")
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return rooms;
    }

    public static Reservation createReservation(ReservationRequest req) {
        if (req.guestName == null || req.guestName.isEmpty()) {
            throw new IllegalArgumentException("Guest name is required");
        }
        if (req.guestEmail == null || req.guestEmail.isEmpty()) {
            throw new IllegalArgumentException("Guest email is required");
        }
        if (req.guestCpf == null || req.guestCpf.isEmpty()) {
            throw new IllegalArgumentException("Guest CPF is required");
        }
        if (req.checkIn == null) {
            throw new IllegalArgumentException("Check-in date is required");
        }
        if (req.checkOut == null) {
            throw new IllegalArgumentException("Check-out date is required");
        }
        if (req.guests <= 0) {
            throw new IllegalArgumentException("Number of guests must be at least 1");
        }
        if (req.ratePlan == null) {
            throw new IllegalArgumentException("Rate plan is required");
        }

        LocalDate checkIn = DateParser.parse(req.checkIn);
        LocalDate checkOut = DateParser.parse(req.checkOut);

        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }

        Room room = findRoom(req.roomNumber);
        if (room == null) {
            throw new NotFoundException("ROOM_NOT_FOUND", "Room not found");
        }

        if (req.guests > room.capacity) {
            throw new UnprocessableException("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity");
        }

        checkRoomAvailability(req.roomNumber, checkIn, checkOut, null);

        RatePlan ratePlan = RatePlan.valueOf(req.ratePlan);
        int nights = PriceCalculator.countNights(checkIn, checkOut);
        BigDecimal subtotal = PriceCalculator.calculateSubtotal(room.nightlyRate, checkIn, checkOut);
        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, ratePlan == RatePlan.NON_REFUNDABLE);
        BigDecimal total = PriceCalculator.calculateTotal(subtotal, discount);

        try (Connection conn = Database.getConnection()) {
            String sql = """
                INSERT INTO reservations
                (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
                 rate_plan, nights, subtotal, discount, total, status, amount_paid, refund)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, req.roomNumber);
                stmt.setString(2, req.guestName);
                stmt.setString(3, req.guestEmail);
                stmt.setString(4, req.guestCpf);
                stmt.setObject(5, checkIn);
                stmt.setObject(6, checkOut);
                stmt.setInt(7, req.guests);
                stmt.setString(8, ratePlan.toString());
                stmt.setInt(9, nights);
                stmt.setBigDecimal(10, subtotal);
                stmt.setBigDecimal(11, discount);
                stmt.setBigDecimal(12, total);
                stmt.setString(13, ReservationStatus.PENDING.toString());
                stmt.setBigDecimal(14, BigDecimal.ZERO);
                stmt.setNull(15, Types.NUMERIC);

                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    long id = rs.getLong("id");
                    return getReservation(id);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Reservation getReservation(long id) {
        try (Connection conn = Database.getConnection()) {
            String sql = "SELECT * FROM reservations WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new NotFoundException("RESERVATION_NOT_FOUND", "Reservation not found");
                    }
                    return rowToReservation(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Reservation confirmReservation(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.status != ReservationStatus.PENDING) {
            throw new ConflictException("OPERATION_NOT_ALLOWED", "Only pending reservations can be confirmed");
        }

        try (Connection conn = Database.getConnection()) {
            String sql = "UPDATE reservations SET status = ?, amount_paid = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, ReservationStatus.CONFIRMED.toString());
                stmt.setBigDecimal(2, reservation.total);
                stmt.setLong(3, id);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        var updated = getReservation(id);

        MessageService.sendMessage("EMAIL", reservation.guestEmail,
            "Reserva confirmada - Pousada Maré Alta",
            generateGuestConfirmationEmail(updated));

        NotificationManager.notifyReservationConfirmed(updated);

        return updated;
    }

    public static Reservation cancelReservation(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.status == ReservationStatus.CHECKED_OUT ||
            reservation.status == ReservationStatus.CANCELLED) {
            throw new ConflictException("OPERATION_NOT_ALLOWED", "Cannot cancel this reservation");
        }

        BigDecimal refund = BigDecimal.ZERO;
        if (reservation.status == ReservationStatus.CONFIRMED ||
            reservation.status == ReservationStatus.CHECKED_IN) {
            if (reservation.ratePlan == RatePlan.STANDARD) {
                refund = reservation.amountPaid;
            }
        }

        try (Connection conn = Database.getConnection()) {
            String sql = "UPDATE reservations SET status = ?, refund = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, ReservationStatus.CANCELLED.toString());
                stmt.setBigDecimal(2, refund.compareTo(BigDecimal.ZERO) > 0 ? refund : null);
                stmt.setLong(3, id);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        var updated = getReservation(id);
        NotificationManager.notifyReservationCancelled(reservation, refund);

        return updated;
    }

    public static Reservation checkIn(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.status != ReservationStatus.CONFIRMED) {
            throw new ConflictException("OPERATION_NOT_ALLOWED", "Only confirmed reservations can check in");
        }

        try (Connection conn = Database.getConnection()) {
            String sql = "UPDATE reservations SET status = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, ReservationStatus.CHECKED_IN.toString());
                stmt.setLong(2, id);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return getReservation(id);
    }

    public static DocumentResponse checkOut(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.status != ReservationStatus.CHECKED_IN) {
            throw new ConflictException("OPERATION_NOT_ALLOWED", "Only checked-in reservations can check out");
        }

        try (Connection conn = Database.getConnection()) {
            String sql = "UPDATE reservations SET status = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, ReservationStatus.CHECKED_OUT.toString());
                stmt.setLong(2, id);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return buildDocument(getReservation(id));
    }

    public static DocumentResponse getDocument(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.status == ReservationStatus.PENDING) {
            throw new ConflictException("OPERATION_NOT_ALLOWED", "Document only available for paid reservations");
        }

        return buildDocument(reservation);
    }

    private static DocumentResponse buildDocument(Reservation reservation) {
        Room room = findRoom(reservation.roomNumber);
        DocumentResponse doc = new DocumentResponse();
        doc.title = "Comprovante de reserva";
        doc.reservationId = reservation.id;
        doc.guestName = reservation.guestName;
        doc.guestCpf = reservation.guestCpf;
        doc.roomNumber = reservation.roomNumber;
        doc.roomType = room.type.toString();
        doc.checkIn = DateParser.format(reservation.checkIn);
        doc.checkOut = DateParser.format(reservation.checkOut);
        doc.nights = reservation.nights;
        doc.subtotal = reservation.subtotal;
        doc.discount = reservation.discount.compareTo(BigDecimal.ZERO) > 0 ? reservation.discount : null;
        doc.total = reservation.total;
        return doc;
    }

    private static void checkRoomAvailability(int roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeReservationId) {
        try (Connection conn = Database.getConnection()) {
            String sql = """
                SELECT COUNT(*) FROM reservations
                WHERE room_number = ?
                AND status != 'CANCELLED'
                AND NOT (check_out <= ? OR check_in >= ?)
                AND (? IS NULL OR id != ?)
                """;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, roomNumber);
                stmt.setObject(2, checkIn);
                stmt.setObject(3, checkOut);
                stmt.setObject(4, excludeReservationId);
                stmt.setObject(5, excludeReservationId);
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        throw new ConflictException("ROOM_UNAVAILABLE", "Room is not available for the requested dates");
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static Room findRoom(int roomNumber) {
        try (Connection conn = Database.getConnection()) {
            String sql = "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, roomNumber);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    return new Room(
                        rs.getInt("number"),
                        RoomType.valueOf(rs.getString("type")),
                        rs.getInt("capacity"),
                        rs.getBigDecimal("nightly_rate")
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static Reservation rowToReservation(ResultSet rs) throws SQLException {
        return new Reservation(
            rs.getLong("id"),
            rs.getInt("room_number"),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            rs.getString("guest_cpf"),
            rs.getObject("check_in", LocalDate.class),
            rs.getObject("check_out", LocalDate.class),
            rs.getInt("guests"),
            RatePlan.valueOf(rs.getString("rate_plan")),
            rs.getInt("nights"),
            rs.getBigDecimal("subtotal"),
            rs.getBigDecimal("discount"),
            rs.getBigDecimal("total"),
            ReservationStatus.valueOf(rs.getString("status")),
            rs.getBigDecimal("amount_paid"),
            rs.getBigDecimal("refund")
        );
    }

    private static String generateGuestConfirmationEmail(Reservation res) {
        Room room = findRoom(res.roomNumber);
        String nightsText = res.nights == 1 ? "1 diária" : res.nights + " diárias";

        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(res.guestName).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");

        sb.append("Comprovante de reserva nº ").append(res.id).append("\n");
        sb.append("Hóspede: ").append(res.guestName).append(" (CPF ").append(res.guestCpf).append(")\n");
        sb.append("Quarto: ").append(res.roomNumber).append(" (").append(room.type).append(")\n");
        sb.append("Entrada: ").append(DateParser.format(res.checkIn)).append(" | ");
        sb.append("Saída: ").append(DateParser.format(res.checkOut)).append(" | ");
        sb.append(nightsText).append("\n");
        sb.append("Diárias: ").append(PriceFormatter.format(res.subtotal)).append("\n");
        if (res.discount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(PriceFormatter.format(res.discount)).append("\n");
        }
        sb.append("Total: ").append(PriceFormatter.format(res.total)).append("\n\n");

        sb.append("A entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!");

        return sb.toString();
    }
}
