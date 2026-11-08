package com.pousada.service;

import com.pousada.database.ReservationDao;
import com.pousada.database.RoomDao;
import com.pousada.database.MessageDao;
import com.pousada.model.Reservation;
import com.pousada.model.ReservationStatus;
import com.pousada.model.RatePlanType;
import com.pousada.model.Room;
import com.pousada.rateplan.RatePlan;
import com.pousada.rateplan.RatePlanException;
import com.pousada.rateplan.RatePlanFactory;
import com.pousada.util.Dates;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReservationService {
    public static Reservation createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                                LocalDate checkIn, LocalDate checkOut, int guests,
                                                RatePlanType ratePlan, String companyCode) throws Exception {
        // Validate room exists
        Room room = RoomDao.findByNumber(roomNumber);
        if (room == null) {
            throw new ApiException("ROOM_NOT_FOUND", "Room not found", 404);
        }

        // Validate dates
        if (checkIn.isAfter(checkOut)) {
            throw new ApiException("INVALID_REQUEST", "Check-out must be after check-in", 400);
        }

        int nights = Dates.countNights(checkIn, checkOut);
        if (nights < 1) {
            throw new ApiException("INVALID_REQUEST", "Invalid date range", 400);
        }

        // Validate guests
        if (guests < 1) {
            throw new ApiException("INVALID_REQUEST", "At least 1 guest is required", 400);
        }
        if (guests > room.capacity) {
            throw new ApiException("CAPACITY_EXCEEDED", "Too many guests for this room", 422);
        }

        // Validate and calculate price using rate plan
        RatePlan plan = RatePlanFactory.create(ratePlan);
        Map<String, Object> context = new HashMap<>();
        context.put("companyCode", companyCode);
        context.put("roomType", room.type);

        try {
            plan.validate(room.capacity, nights, checkIn, checkOut, guests, context);
        } catch (RatePlanException e) {
            throw new ApiException("INVALID_REQUEST", e.getMessage(), 400);
        }

        // Check room availability
        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            List<Reservation> existing = ReservationDao.findByRoomAndNight(roomNumber, date);
            if (!existing.isEmpty()) {
                throw new ApiException("ROOM_UNAVAILABLE", "Room is not available for the selected dates", 409);
            }
        }

        // Calculate price
        RatePlan.PriceCalculation pricing = plan.calculate(room.nightlyRate, nights, checkIn, checkOut, context);

        // Create reservation
        Reservation reservation = ReservationDao.insert(
                roomNumber, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan, companyCode,
                nights, pricing.subtotal, pricing.discount, pricing.total
        );

        return reservation;
    }

    public static Reservation getReservation(int id) throws Exception {
        Reservation reservation = ReservationDao.findById(id);
        if (reservation == null) {
            throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found", 404);
        }
        return reservation;
    }

    public static Reservation confirmReservation(int id, BigDecimal amountPaid) throws Exception {
        Reservation reservation = getReservation(id);

        if (reservation.status != ReservationStatus.PENDING) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Can only confirm a pending reservation", 409);
        }

        ReservationDao.updateStatus(id, ReservationStatus.CONFIRMED);
        ReservationDao.updateAmountPaid(id, amountPaid);

        // Send confirmation email
        String receiptText = generateReceipt(reservation);
        String emailBody = """
                Olá, %s!

                Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

                %s

                A entrada é a partir das 14h, e a saída, até as 12h.
                Até breve!""".formatted(reservation.guestName, receiptText);

        MessageDao.insert("EMAIL", reservation.guestEmail,
                "Reserva confirmada - Pousada Maré Alta", emailBody);

        return getReservation(id);
    }

    public static Reservation cancelReservation(int id) throws Exception {
        Reservation reservation = getReservation(id);

        if (reservation.status == ReservationStatus.CHECKED_OUT || reservation.status == ReservationStatus.CANCELLED) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Can only cancel before check-out", 409);
        }

        RatePlan plan = RatePlanFactory.create(reservation.ratePlan);
        BigDecimal refund = plan.calculateRefund(reservation.amountPaid);

        ReservationDao.updateStatus(id, ReservationStatus.CANCELLED);
        ReservationDao.updateRefund(id, refund);

        return getReservation(id);
    }

    public static Reservation checkInReservation(int id) throws Exception {
        Reservation reservation = getReservation(id);

        if (reservation.status != ReservationStatus.CONFIRMED) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Can only check in a confirmed reservation", 409);
        }

        ReservationDao.updateStatus(id, ReservationStatus.CHECKED_IN);
        return getReservation(id);
    }

    public static Reservation checkOutReservation(int id) throws Exception {
        Reservation reservation = getReservation(id);

        if (reservation.status != ReservationStatus.CHECKED_IN) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Can only check out a checked-in reservation", 409);
        }

        ReservationDao.updateStatus(id, ReservationStatus.CHECKED_OUT);
        return getReservation(id);
    }

    public static String generateReceipt(Reservation reservation) throws Exception {
        Room room = RoomDao.findByNumber(reservation.roomNumber);
        String nights = reservation.nights == 1 ? "1 diária" : reservation.nights + " diárias";
        String discount = reservation.discount.compareTo(BigDecimal.ZERO) > 0 ?
                "\nDesconto: R$ " + formatMoney(reservation.discount) : "";

        return String.format(
                "Comprovante de reserva nº %d%n" +
                        "Hóspede: %s (CPF %s)%n" +
                        "Quarto: %d (%s)%n" +
                        "Entrada: %s | Saída: %s | %s%n" +
                        "Diárias: R$ %s%s%n" +
                        "Total: R$ %s",
                reservation.id,
                reservation.guestName,
                reservation.guestCpf,
                reservation.roomNumber,
                room.type,
                Dates.formatBr(reservation.checkIn),
                Dates.formatBr(reservation.checkOut),
                nights,
                formatMoney(reservation.subtotal),
                discount,
                formatMoney(reservation.total)
        );
    }

    private static String formatMoney(BigDecimal value) {
        return String.format("%.2f", value).replace(".", ",");
    }
}
