package com.pousadamarealta.service;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.messaging.OutgoingMessage;
import com.pousadamarealta.util.CpfFormatter;
import com.pousadamarealta.util.DateFormatter;
import com.pousadamarealta.util.MoneyFormatter;
import com.pousadamarealta.util.PriceCalculator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class ReservationService {
    private final Map<Long, Reservation> reservations = new HashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);
    private final RoomService roomService;
    private final MessageService messageService;

    public ReservationService(RoomService roomService, MessageService messageService) {
        this.roomService = roomService;
        this.messageService = messageService;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlan ratePlan) {
        Room room = roomService.getRoomByNumber(roomNumber)
            .orElseThrow(() -> new RoomNotFoundException());

        if (guests < 1) {
            throw new InvalidRequestException("Number of guests must be at least 1");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException();
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        if (isRoomUnavailable(roomNumber, checkIn, checkOut, null)) {
            throw new RoomUnavailableException();
        }

        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal subtotal = PriceCalculator.calculateSubtotal(room, checkIn, checkOut);
        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = PriceCalculator.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation(
            nextId.getAndIncrement(),
            roomNumber,
            guestName,
            guestEmail,
            guestCpf,
            checkIn,
            checkOut,
            guests,
            ratePlan,
            nights,
            subtotal,
            discount,
            total
        );

        reservations.put(reservation.getId(), reservation);
        return reservation;
    }

    public Reservation getReservation(long id) {
        return Optional.ofNullable(reservations.get(id))
            .orElseThrow(() -> new ReservationNotFoundException());
    }

    public Reservation confirmReservation(long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canConfirm()) {
            throw new OperationNotAllowedException();
        }

        if (isRoomUnavailable(reservation.getRoomNumber(), reservation.getCheckIn(),
                              reservation.getCheckOut(), reservation.getId())) {
            throw new RoomUnavailableException();
        }

        reservation.confirm();
        sendConfirmationEmail(reservation);
        return reservation;
    }

    public Reservation cancelReservation(long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canCancel()) {
            throw new OperationNotAllowedException();
        }

        reservation.cancel();
        return reservation;
    }

    public Reservation checkIn(long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canCheckIn()) {
            throw new OperationNotAllowedException();
        }

        reservation.checkIn();
        return reservation;
    }

    public Reservation checkOut(long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canCheckOut()) {
            throw new OperationNotAllowedException();
        }

        reservation.checkOut();
        return reservation;
    }

    public String generateDocument(long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.isPaid()) {
            throw new OperationNotAllowedException();
        }

        return buildReceipt(reservation);
    }

    public List<Reservation> getReservationsForNight(LocalDate night) {
        return reservations.values().stream()
            .filter(r -> r.isActive() && r.isPaid() && r.occupiesNight(night))
            .sorted(Comparator.comparing(Reservation::getRoomNumber))
            .toList();
    }

    public List<Reservation> getReservationsForMonth(int year, int month) {
        return reservations.values().stream()
            .filter(r -> {
                LocalDate checkIn = r.getCheckIn();
                return checkIn.getYear() == year && checkIn.getMonthValue() == month;
            })
            .filter(r -> {
                BigDecimal netAmount = r.getAmountPaid();
                if (r.getRefund() != null) {
                    netAmount = netAmount.subtract(r.getRefund());
                }
                return netAmount.compareTo(BigDecimal.ZERO) > 0;
            })
            .sorted(Comparator.comparing(Reservation::getCheckIn)
                             .thenComparing(Reservation::getId))
            .toList();
    }

    private boolean isRoomUnavailable(String roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeId) {
        return reservations.values().stream()
            .filter(r -> r.isActive())
            .filter(r -> r.getRoomNumber().equals(roomNumber))
            .filter(r -> excludeId == null || r.getId() != excludeId)
            .anyMatch(r -> datesOverlap(checkIn, checkOut, r.getCheckIn(), r.getCheckOut()));
    }

    private boolean datesOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    private void sendConfirmationEmail(Reservation reservation) {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = buildConfirmationEmail(reservation);

        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL,
            reservation.getGuestEmail(),
            subject,
            body
        );

        messageService.send(message);
    }

    private String buildConfirmationEmail(Reservation reservation) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(reservation.getGuestName()).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        sb.append(buildReceipt(reservation));
        sb.append("\n\nA entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!");
        return sb.toString();
    }

    private String buildReceipt(Reservation reservation) {
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber()).orElseThrow();

        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName());
        sb.append(" (CPF ").append(CpfFormatter.format(reservation.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber());
        sb.append(" (").append(room.getType()).append(")\n");
        sb.append("Entrada: ").append(DateFormatter.format(reservation.getCheckIn()));
        sb.append(" | Saída: ").append(DateFormatter.format(reservation.getCheckOut()));
        sb.append(" | ").append(reservation.getNights()).append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(MoneyFormatter.format(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(MoneyFormatter.format(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(MoneyFormatter.format(reservation.getTotal()));

        return sb.toString();
    }

    public static class RoomNotFoundException extends RuntimeException {}
    public static class ReservationNotFoundException extends RuntimeException {}
    public static class RoomUnavailableException extends RuntimeException {}
    public static class CapacityExceededException extends RuntimeException {}
    public static class OperationNotAllowedException extends RuntimeException {}
    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }
}
