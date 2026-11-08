package com.pousadamarealta.service;

import com.pousadamarealta.config.Database;
import com.pousadamarealta.domain.*;
import com.pousadamarealta.exception.ApiException;
import com.pousadamarealta.service.rateplan.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class ReservationService {
    private final Database database;
    private final RoomService roomService;
    private final MessageService messageService;
    private final Map<RatePlan, RatePlanStrategy> ratePlanStrategies;

    public ReservationService(Database database, RoomService roomService, MessageService messageService) {
        this.database = database;
        this.roomService = roomService;
        this.messageService = messageService;
        this.ratePlanStrategies = Map.of(
            RatePlan.STANDARD, new StandardRatePlan(),
            RatePlan.NON_REFUNDABLE, new NonRefundableRatePlan(),
            RatePlan.CORPORATE, new CorporateRatePlan(),
            RatePlan.WEEKEND_PACKAGE, new WeekendPackageRatePlan(),
            RatePlan.LONG_STAY, new LongStayRatePlan()
        );
    }

    public Reservation createReservation(Reservation reservation) {
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());

        int nights = (int) ChronoUnit.DAYS.between(reservation.getCheckIn(), reservation.getCheckOut());
        reservation.setNights(nights);

        if (reservation.getGuests() < 1) {
            throw new ApiException(400, "INVALID_REQUEST", "At least one guest is required");
        }

        if (reservation.getGuests() > room.getCapacity()) {
            throw new ApiException(422, "CAPACITY_EXCEEDED",
                "Room capacity exceeded. Max capacity: " + room.getCapacity());
        }

        RatePlanStrategy strategy = ratePlanStrategies.get(reservation.getRatePlan());
        strategy.validate(reservation, room);
        strategy.calculatePricing(reservation, room);

        checkRoomAvailability(reservation.getRoomNumber(), reservation.getCheckIn(),
            reservation.getCheckOut(), null);

        String sql = """
            INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf,
                check_in, check_out, guests, rate_plan, company_code, nights, subtotal,
                discount, total, status, amount_paid)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
        """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, reservation.getRoomNumber());
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
            stmt.setString(14, ReservationStatus.PENDING.name());
            stmt.setBigDecimal(15, BigDecimal.ZERO);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    reservation.setId(rs.getLong("id"));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create reservation", e);
        }

        return reservation;
    }

    public Reservation getReservation(Long id) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                   guests, rate_plan, company_code, nights, subtotal, discount, total,
                   status, amount_paid, refund
            FROM reservations WHERE id = ?
        """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapReservation(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to get reservation", e);
        }

        throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found: " + id);
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED",
                "Only pending reservations can be confirmed");
        }

        updateReservationStatus(id, ReservationStatus.CONFIRMED);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        updateAmountPaid(id, reservation.getTotal());

        sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() == ReservationStatus.CHECKED_IN ||
            reservation.getStatus() == ReservationStatus.CHECKED_OUT ||
            reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED",
                "Cannot cancel reservation in status: " + reservation.getStatus());
        }

        RatePlanStrategy strategy = ratePlanStrategies.get(reservation.getRatePlan());
        BigDecimal refund = strategy.calculateRefund(reservation);

        updateReservationStatus(id, ReservationStatus.CANCELLED);
        updateRefund(id, refund);

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setRefund(refund);

        return reservation;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED",
                "Only confirmed reservations can check in");
        }

        updateReservationStatus(id, ReservationStatus.CHECKED_IN);
        reservation.setStatus(ReservationStatus.CHECKED_IN);

        return reservation;
    }

    public Document checkOut(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED",
                "Only checked-in reservations can check out");
        }

        updateReservationStatus(id, ReservationStatus.CHECKED_OUT);
        reservation.setStatus(ReservationStatus.CHECKED_OUT);

        return generateDocument(reservation);
    }

    public Document getDocument(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() == ReservationStatus.PENDING) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED",
                "Document is only available for paid reservations");
        }

        return generateDocument(reservation);
    }

    private Document generateDocument(Reservation reservation) {
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());

        Document document = new Document();
        document.setTitle("Comprovante de reserva");
        document.setReservationId(reservation.getId());
        document.setGuestName(reservation.getGuestName());
        document.setGuestCpf(reservation.getGuestCpf());
        document.setRoomNumber(reservation.getRoomNumber());
        document.setRoomType(room.getType());
        document.setCheckIn(reservation.getCheckIn());
        document.setCheckOut(reservation.getCheckOut());
        document.setNights(reservation.getNights());
        document.setSubtotal(reservation.getSubtotal());
        document.setDiscount(reservation.getDiscount());
        document.setTotal(reservation.getTotal());

        return document;
    }

    private void sendConfirmationEmail(Reservation reservation) {
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());

        String receipt = formatReceipt(reservation, room);

        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!""", reservation.getGuestName(), receipt);

        Message message = new Message(
            "EMAIL",
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            body
        );

        messageService.sendMessage(message);
    }

    private String formatReceipt(Reservation reservation, Room room) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        StringBuilder receipt = new StringBuilder();
        receipt.append(String.format("Comprovante de reserva nº %d\n", reservation.getId()));
        receipt.append(String.format("Hóspede: %s (CPF %s)\n",
            reservation.getGuestName(), formatCpf(reservation.getGuestCpf())));
        receipt.append(String.format("Quarto: %s (%s)\n",
            reservation.getRoomNumber(), room.getType().name()));
        receipt.append(String.format("Entrada: %s | Saída: %s | %s\n",
            reservation.getCheckIn().format(formatter),
            reservation.getCheckOut().format(formatter),
            reservation.getNights() == 1 ? "1 diária" : reservation.getNights() + " diárias"));
        receipt.append(String.format("Diárias: %s\n", formatMoney(reservation.getSubtotal())));

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            receipt.append(String.format("Desconto: %s\n", formatMoney(reservation.getDiscount())));
        }

        receipt.append(String.format("Total: %s", formatMoney(reservation.getTotal())));

        return receipt.toString();
    }

    private String formatCpf(String cpf) {
        cpf = cpf.replaceAll("[^0-9]", "");
        if (cpf.length() == 11) {
            return String.format("%s.%s.%s-%s",
                cpf.substring(0, 3),
                cpf.substring(3, 6),
                cpf.substring(6, 9),
                cpf.substring(9, 11));
        }
        return cpf;
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("R$ %,.2f", amount).replace(",", "X").replace(".", ",").replace("X", ".");
    }

    private void checkRoomAvailability(String roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeReservationId) {
        String sql = """
            SELECT COUNT(*) FROM reservations
            WHERE room_number = ?
            AND status != 'CANCELLED'
            AND check_in < ?
            AND check_out > ?
        """;

        if (excludeReservationId != null) {
            sql += " AND id != ?";
        }

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));

            if (excludeReservationId != null) {
                stmt.setLong(4, excludeReservationId);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new ApiException(409, "ROOM_UNAVAILABLE",
                        "Room is not available for the selected dates");
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to check room availability", e);
        }
    }

    private void updateReservationStatus(Long id, ReservationStatus status) {
        String sql = "UPDATE reservations SET status = ? WHERE id = ?";

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setLong(2, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation status", e);
        }
    }

    private void updateAmountPaid(Long id, BigDecimal amount) {
        String sql = "UPDATE reservations SET amount_paid = ? WHERE id = ?";

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBigDecimal(1, amount);
            stmt.setLong(2, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update amount paid", e);
        }
    }

    private void updateRefund(Long id, BigDecimal refund) {
        String sql = "UPDATE reservations SET refund = ? WHERE id = ?";

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBigDecimal(1, refund);
            stmt.setLong(2, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update refund", e);
        }
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        Reservation reservation = new Reservation();
        reservation.setId(rs.getLong("id"));
        reservation.setRoomNumber(rs.getString("room_number"));
        reservation.setGuestName(rs.getString("guest_name"));
        reservation.setGuestEmail(rs.getString("guest_email"));
        reservation.setGuestCpf(rs.getString("guest_cpf"));
        reservation.setCheckIn(rs.getDate("check_in").toLocalDate());
        reservation.setCheckOut(rs.getDate("check_out").toLocalDate());
        reservation.setGuests(rs.getInt("guests"));
        reservation.setRatePlan(RatePlan.valueOf(rs.getString("rate_plan")));
        reservation.setCompanyCode(rs.getString("company_code"));
        reservation.setNights(rs.getInt("nights"));
        reservation.setSubtotal(rs.getBigDecimal("subtotal"));
        reservation.setDiscount(rs.getBigDecimal("discount"));
        reservation.setTotal(rs.getBigDecimal("total"));
        reservation.setStatus(ReservationStatus.valueOf(rs.getString("status")));
        reservation.setAmountPaid(rs.getBigDecimal("amount_paid"));

        BigDecimal refund = rs.getBigDecimal("refund");
        if (!rs.wasNull()) {
            reservation.setRefund(refund);
        }

        return reservation;
    }
}
