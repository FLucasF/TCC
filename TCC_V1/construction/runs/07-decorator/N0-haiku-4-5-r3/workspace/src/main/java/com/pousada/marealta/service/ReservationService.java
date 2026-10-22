package com.pousada.marealta.service;

import com.pousada.marealta.db.Database;
import com.pousada.marealta.exception.CapacityExceededException;
import com.pousada.marealta.exception.RoomUnavailableException;
import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.model.Room;
import com.pousada.marealta.util.PriceCalculator;

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

public class ReservationService {
    public static Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                                 String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                                 int guests, String ratePlan) throws SQLException, CapacityExceededException, RoomUnavailableException {
        var room = RoomService.getRoomById(roomNumber);
        if (room == null) {
            return null;
        }

        if (guests < 1) {
            throw new IllegalArgumentException("Guests must be at least 1");
        }

        if (guests > room.capacity) {
            throw new CapacityExceededException("Room capacity exceeded");
        }

        if (!checkIn.isBefore(checkOut)) {
            throw new IllegalArgumentException("Check-in must be before check-out");
        }

        if (isRoomUnavailable(roomNumber, checkIn, checkOut)) {
            throw new RoomUnavailableException("Room is unavailable for these dates");
        }

        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        var priceBreakdown = PriceCalculator.calculatePrice(room.nightlyRate, checkIn, checkOut, ratePlan);

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     """
                     INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf,
                                             check_in, check_out, guests, rate_plan, nights,
                                             subtotal, discount, total, status, amount_paid)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', 0)
                     """,
                     Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, roomNumber);
            stmt.setString(2, guestName);
            stmt.setString(3, guestEmail);
            stmt.setString(4, guestCpf);
            stmt.setDate(5, Date.valueOf(checkIn));
            stmt.setDate(6, Date.valueOf(checkOut));
            stmt.setInt(7, guests);
            stmt.setString(8, ratePlan);
            stmt.setInt(9, nights);
            stmt.setBigDecimal(10, priceBreakdown.subtotal);
            stmt.setBigDecimal(11, priceBreakdown.discount);
            stmt.setBigDecimal(12, priceBreakdown.total);

            stmt.executeUpdate();

            var keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                int id = keys.getInt(1);
                return getReservationById(id);
            }
        }

        return null;
    }

    public static Reservation getReservationById(int id) throws SQLException {
        try (Connection conn = Database.getConnection();
             var stmt = conn.createStatement()) {

            var result = stmt.executeQuery(
                    "SELECT * FROM reservations WHERE id = " + id);

            if (result.next()) {
                return mapReservation(result);
            }
        }

        return null;
    }

    public static Reservation confirmReservation(int id, BigDecimal amountPaid) throws SQLException {
        var reservation = getReservationById(id);
        if (reservation == null || !reservation.status.equals("PENDING")) {
            return null;
        }

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE reservations SET status = 'CONFIRMED', amount_paid = ? WHERE id = ?")) {

            stmt.setBigDecimal(1, amountPaid);
            stmt.setInt(2, id);
            stmt.executeUpdate();

            var updated = getReservationById(id);

            try {
                var comprovante = generateComprovante(updated);
                MessageService.sendMessage(
                        updated.guestEmail,
                        "Reserva confirmada - Pousada Maré Alta",
                        formatConfirmationEmail(updated, comprovante)
                );
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            return updated;
        }
    }

    public static Reservation cancelReservation(int id) throws SQLException {
        var reservation = getReservationById(id);
        if (reservation == null || reservation.status.equals("CHECKED_OUT") || reservation.status.equals("CANCELLED")) {
            return null;
        }

        BigDecimal refund = null;
        if (reservation.status.equals("CONFIRMED") || reservation.status.equals("CHECKED_IN")) {
            if ("STANDARD".equals(reservation.ratePlan)) {
                refund = reservation.amountPaid;
            } else {
                refund = BigDecimal.ZERO;
            }
        }

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE reservations SET status = 'CANCELLED', refund = ? WHERE id = ?")) {

            stmt.setBigDecimal(1, refund);
            stmt.setInt(2, id);
            stmt.executeUpdate();

            return getReservationById(id);
        }
    }

    public static Reservation checkIn(int id) throws SQLException {
        var reservation = getReservationById(id);
        if (reservation == null || !reservation.status.equals("CONFIRMED")) {
            return null;
        }

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE reservations SET status = 'CHECKED_IN' WHERE id = ?")) {

            stmt.setInt(1, id);
            stmt.executeUpdate();

            return getReservationById(id);
        }
    }

    public static Reservation checkOut(int id) throws SQLException {
        var reservation = getReservationById(id);
        if (reservation == null || !reservation.status.equals("CHECKED_IN")) {
            return null;
        }

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE reservations SET status = 'CHECKED_OUT' WHERE id = ?")) {

            stmt.setInt(1, id);
            stmt.executeUpdate();

            return getReservationById(id);
        }
    }

    public static String generateComprovante(Reservation reservation) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.id).append("\n");
        sb.append("Hóspede: ").append(reservation.guestName).append(" (CPF ").append(reservation.guestCpf).append(")\n");

        var room = getRoomTypeForNumber(reservation.roomNumber);
        sb.append("Quarto: ").append(reservation.roomNumber).append(" (").append(room).append(")\n");

        String nights = reservation.nights == 1 ? "1 diária" : reservation.nights + " diárias";
        sb.append("Entrada: ").append(formatDate(reservation.checkIn))
                .append(" | Saída: ").append(formatDate(reservation.checkOut))
                .append(" | ").append(nights).append("\n");

        sb.append("Diárias: ").append(formatMoney(reservation.subtotal)).append("\n");

        if (reservation.discount.signum() > 0) {
            sb.append("Desconto: ").append(formatMoney(reservation.discount)).append("\n");
        }

        sb.append("Total: ").append(formatMoney(reservation.total));

        return sb.toString();
    }

    private static String formatConfirmationEmail(Reservation reservation, String comprovante) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(reservation.guestName).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        sb.append(comprovante).append("\n\n");
        sb.append("A entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!");
        return sb.toString();
    }

    private static String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private static String formatMoney(BigDecimal amount) {
        return String.format("R$ %.2f", amount);
    }

    private static String getRoomTypeForNumber(int roomNumber) {
        try {
            var room = RoomService.getRoomById(roomNumber);
            return room != null ? room.type : "UNKNOWN";
        } catch (SQLException e) {
            return "UNKNOWN";
        }
    }

    private static boolean isRoomUnavailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        try (Connection conn = Database.getConnection();
             var stmt = conn.createStatement()) {

            var result = stmt.executeQuery(
                    "SELECT COUNT(*) as count FROM reservations " +
                    "WHERE room_number = " + roomNumber + " " +
                    "AND status != 'CANCELLED' " +
                    "AND check_in < '" + checkOut + "' " +
                    "AND check_out > '" + checkIn + "'");

            if (result.next()) {
                return result.getInt("count") > 0;
            }
        }

        return false;
    }

    private static Reservation mapReservation(ResultSet result) throws SQLException {
        return new Reservation(
                result.getInt("id"),
                result.getInt("room_number"),
                result.getString("guest_name"),
                result.getString("guest_email"),
                result.getString("guest_cpf"),
                result.getDate("check_in").toLocalDate(),
                result.getDate("check_out").toLocalDate(),
                result.getInt("guests"),
                result.getString("rate_plan"),
                result.getInt("nights"),
                result.getBigDecimal("subtotal"),
                result.getBigDecimal("discount"),
                result.getBigDecimal("total"),
                result.getString("status"),
                result.getBigDecimal("amount_paid"),
                result.getBigDecimal("refund")
        );
    }
}
