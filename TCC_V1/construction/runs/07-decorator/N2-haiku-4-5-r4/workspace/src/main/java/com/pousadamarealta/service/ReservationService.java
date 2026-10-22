package com.pousadamarealta.service;

import com.pousadamarealta.db.Database;
import com.pousadamarealta.model.*;
import com.pousadamarealta.util.Pricing;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationService {
    private final Database database;
    private final MessageService messageService;

    public ReservationService(Database database, MessageService messageService) {
        this.database = database;
        this.messageService = messageService;
    }

    public List<Room> getAllRooms() {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = database.getConnection()) {
            String sql = "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number";
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    Room room = new Room(
                            rs.getInt("number"),
                            rs.getString("type"),
                            rs.getInt("capacity"),
                            rs.getBigDecimal("nightly_rate")
                    );
                    rooms.add(room);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return rooms;
    }

    public Room getRoomByNumber(int number) throws SQLException {
        try (Connection conn = database.getConnection()) {
            String sql = "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, number);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return new Room(
                            rs.getInt("number"),
                            rs.getString("type"),
                            rs.getInt("capacity"),
                            rs.getBigDecimal("nightly_rate")
                    );
                }
            }
        }
        return null;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                        LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan)
            throws SQLException {
        Room room = getRoomByNumber(roomNumber);
        if (room == null) {
            throw new RuntimeException("ROOM_NOT_FOUND");
        }

        if (guests < 1 || guests > room.capacity) {
            throw new RuntimeException(guests > room.capacity ? "CAPACITY_EXCEEDED" : "INVALID_REQUEST");
        }

        if (checkOut.isBefore(checkIn) || checkOut.equals(checkIn)) {
            throw new RuntimeException("INVALID_REQUEST");
        }

        if (isRoomUnavailable(roomNumber, checkIn, checkOut)) {
            throw new RuntimeException("ROOM_UNAVAILABLE");
        }

        int nights = Pricing.getNights(checkIn, checkOut);
        BigDecimal subtotal = Pricing.calculateSubtotal(room.nightlyRate, checkIn, checkOut);
        BigDecimal discount = Pricing.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = Pricing.calculateTotal(subtotal, discount);

        try (Connection conn = database.getConnection()) {
            String sql = """
                INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf,
                                         check_in, check_out, guests, rate_plan, nights,
                                         subtotal, discount, total, status, amount_paid)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', 0)
                """;
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, roomNumber);
                stmt.setString(2, guestName);
                stmt.setString(3, guestEmail);
                stmt.setString(4, guestCpf);
                stmt.setDate(5, Date.valueOf(checkIn));
                stmt.setDate(6, Date.valueOf(checkOut));
                stmt.setInt(7, guests);
                stmt.setString(8, ratePlan);
                stmt.setInt(9, nights);
                stmt.setBigDecimal(10, subtotal);
                stmt.setBigDecimal(11, discount);
                stmt.setBigDecimal(12, total);
                stmt.executeUpdate();

                ResultSet keys = stmt.getGeneratedKeys();
                int id = 0;
                if (keys.next()) {
                    id = keys.getInt(1);
                }

                return getReservationById(id);
            }
        }
    }

    public Reservation getReservationById(int id) throws SQLException {
        try (Connection conn = database.getConnection()) {
            String sql = """
                SELECT id, room_number, guest_name, guest_email, guest_cpf,
                       check_in, check_out, guests, rate_plan, nights,
                       subtotal, discount, total, status, amount_paid, refund
                FROM reservations WHERE id = ?
                """;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return new Reservation(
                            rs.getInt("id"),
                            rs.getInt("room_number"),
                            rs.getString("guest_name"),
                            rs.getString("guest_email"),
                            rs.getString("guest_cpf"),
                            rs.getDate("check_in").toLocalDate(),
                            rs.getDate("check_out").toLocalDate(),
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
            }
        }
        return null;
    }

    public Reservation confirmReservation(int reservationId) throws SQLException {
        Reservation reservation = getReservationById(reservationId);
        if (reservation == null) {
            throw new RuntimeException("RESERVATION_NOT_FOUND");
        }

        if (!reservation.status.equals(ReservationStatus.PENDING)) {
            throw new RuntimeException("OPERATION_NOT_ALLOWED");
        }

        try (Connection conn = database.getConnection()) {
            String sql = "UPDATE reservations SET status = 'CONFIRMED', amount_paid = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setBigDecimal(1, reservation.total);
                stmt.setInt(2, reservationId);
                stmt.executeUpdate();
            }
        }

        Reservation updated = getReservationById(reservationId);
        String receipt = generateReceipt(updated);
        messageService.sendConfirmationEmail(reservation.guestName, reservation.guestEmail, receipt);

        return updated;
    }

    public Reservation checkIn(int reservationId) throws SQLException {
        Reservation reservation = getReservationById(reservationId);
        if (reservation == null) {
            throw new RuntimeException("RESERVATION_NOT_FOUND");
        }

        if (!reservation.status.equals(ReservationStatus.CONFIRMED)) {
            throw new RuntimeException("OPERATION_NOT_ALLOWED");
        }

        try (Connection conn = database.getConnection()) {
            String sql = "UPDATE reservations SET status = 'CHECKED_IN' WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, reservationId);
                stmt.executeUpdate();
            }
        }

        return getReservationById(reservationId);
    }

    public Reservation checkOut(int reservationId) throws SQLException {
        Reservation reservation = getReservationById(reservationId);
        if (reservation == null) {
            throw new RuntimeException("RESERVATION_NOT_FOUND");
        }

        if (!reservation.status.equals(ReservationStatus.CHECKED_IN)) {
            throw new RuntimeException("OPERATION_NOT_ALLOWED");
        }

        try (Connection conn = database.getConnection()) {
            String sql = "UPDATE reservations SET status = 'CHECKED_OUT' WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, reservationId);
                stmt.executeUpdate();
            }
        }

        return getReservationById(reservationId);
    }

    public Reservation cancelReservation(int reservationId) throws SQLException {
        Reservation reservation = getReservationById(reservationId);
        if (reservation == null) {
            throw new RuntimeException("RESERVATION_NOT_FOUND");
        }

        if (!reservation.status.equals(ReservationStatus.PENDING) &&
            !reservation.status.equals(ReservationStatus.CONFIRMED)) {
            throw new RuntimeException("OPERATION_NOT_ALLOWED");
        }

        BigDecimal refund = BigDecimal.ZERO;
        if (reservation.status.equals(ReservationStatus.CONFIRMED)) {
            if (reservation.ratePlan.equals(RatePlan.STANDARD)) {
                refund = reservation.amountPaid;
            }
        }

        try (Connection conn = database.getConnection()) {
            String sql = "UPDATE reservations SET status = 'CANCELLED', refund = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setBigDecimal(1, refund);
                stmt.setInt(2, reservationId);
                stmt.executeUpdate();
            }
        }

        return getReservationById(reservationId);
    }

    public boolean isRoomUnavailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        try (Connection conn = database.getConnection()) {
            String sql = """
                SELECT COUNT(*) FROM reservations
                WHERE room_number = ?
                AND status != 'CANCELLED'
                AND check_in < ?
                AND check_out > ?
                """;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, roomNumber);
                stmt.setDate(2, Date.valueOf(checkOut));
                stmt.setDate(3, Date.valueOf(checkIn));
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private String generateReceipt(Reservation reservation) throws SQLException {
        Room room = getRoomByNumber(reservation.roomNumber);
        String nightsText = reservation.nights == 1 ? "1 diária" : reservation.nights + " diárias";

        StringBuilder receipt = new StringBuilder();
        receipt.append("Comprovante de reserva nº ").append(reservation.id).append("\n");
        receipt.append("Hóspede: ").append(reservation.guestName)
               .append(" (CPF ").append(formatCpf(reservation.guestCpf)).append(")\n");
        receipt.append("Quarto: ").append(reservation.roomNumber).append(" (").append(room.type).append(")\n");
        receipt.append("Entrada: ").append(formatDate(reservation.checkIn))
               .append(" | Saída: ").append(formatDate(reservation.checkOut))
               .append(" | ").append(nightsText).append("\n");
        receipt.append("Diárias: ").append(formatMoney(reservation.subtotal)).append("\n");

        if (reservation.discount.compareTo(BigDecimal.ZERO) > 0) {
            receipt.append("Desconto: ").append(formatMoney(reservation.discount)).append("\n");
        }

        receipt.append("Total: ").append(formatMoney(reservation.total));

        return receipt.toString();
    }

    public String getReceipt(int reservationId) throws SQLException {
        Reservation reservation = getReservationById(reservationId);
        if (reservation == null) {
            throw new RuntimeException("RESERVATION_NOT_FOUND");
        }

        if (!reservation.status.equals(ReservationStatus.CONFIRMED) &&
            !reservation.status.equals(ReservationStatus.CHECKED_IN) &&
            !reservation.status.equals(ReservationStatus.CHECKED_OUT)) {
            throw new RuntimeException("OPERATION_NOT_ALLOWED");
        }

        return generateReceipt(reservation);
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }

    private String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private String formatMoney(BigDecimal amount) {
        return "R$ " + String.format("%.2f", amount).replace(".", ",");
    }
}
