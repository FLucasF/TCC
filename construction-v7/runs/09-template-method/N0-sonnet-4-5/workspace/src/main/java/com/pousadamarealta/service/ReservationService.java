package com.pousadamarealta.service;

import com.pousadamarealta.model.*;
import com.pousadamarealta.util.Formatter;
import com.pousadamarealta.util.PricingCalculator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class ReservationService {
    private final Map<Long, Reservation> reservations;
    private final AtomicLong nextId;
    private final RoomService roomService;
    private final MessageService messageService;

    public ReservationService(RoomService roomService, MessageService messageService) {
        this.reservations = new HashMap<>();
        this.nextId = new AtomicLong(1);
        this.roomService = roomService;
        this.messageService = messageService;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                         String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                         int guests, RatePlan ratePlan) {
        Room room = roomService.findByNumber(roomNumber)
                .orElseThrow(() -> new RoomNotFoundException("Room not found"));

        if (guests < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException("Room capacity exceeded");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        if (isRoomUnavailable(roomNumber, checkIn, checkOut, null)) {
            throw new RoomUnavailableException("Room is not available for the selected dates");
        }

        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal subtotal = PricingCalculator.calculateSubtotal(room.getNightlyRate(), checkIn, checkOut);
        BigDecimal discount = PricingCalculator.calculateDiscount(subtotal, ratePlan == RatePlan.NON_REFUNDABLE);
        BigDecimal total = PricingCalculator.calculateTotal(subtotal, discount);

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

    public Reservation confirmReservation(long id) {
        Reservation reservation = findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException("Only pending reservations can be confirmed");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());

        sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation cancelReservation(long id) {
        Reservation reservation = findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        if (reservation.getStatus() == ReservationStatus.CHECKED_IN ||
                reservation.getStatus() == ReservationStatus.CHECKED_OUT ||
                reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new OperationNotAllowedException("Cannot cancel reservation in current status");
        }

        BigDecimal refundAmount = BigDecimal.ZERO;
        if (reservation.getRatePlan() == RatePlan.STANDARD) {
            refundAmount = reservation.getAmountPaid();
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setRefund(refundAmount);

        return reservation;
    }

    public Reservation checkIn(long id) {
        Reservation reservation = findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Only confirmed reservations can check in");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        return reservation;
    }

    public Reservation checkOut(long id) {
        Reservation reservation = findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException("Only checked-in reservations can check out");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        return reservation;
    }

    public Document generateDocument(long id) {
        Reservation reservation = findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        if (reservation.getStatus() == ReservationStatus.PENDING ||
                reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new OperationNotAllowedException("Document only available for paid reservations");
        }

        Room room = roomService.findByNumber(reservation.getRoomNumber()).orElseThrow();

        return new Document(
                "Comprovante de reserva",
                reservation.getId(),
                reservation.getGuestName(),
                reservation.getGuestCpf(),
                reservation.getRoomNumber(),
                room.getType(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getNights(),
                reservation.getSubtotal(),
                reservation.getDiscount(),
                reservation.getTotal()
        );
    }

    public Optional<Reservation> findById(long id) {
        return Optional.ofNullable(reservations.get(id));
    }

    public List<Reservation> getAllReservations() {
        return new ArrayList<>(reservations.values());
    }

    private boolean isRoomUnavailable(String roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeId) {
        return reservations.values().stream()
                .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
                .filter(r -> excludeId == null || r.getId() != excludeId)
                .filter(r -> r.getRoomNumber().equals(roomNumber))
                .anyMatch(r -> datesOverlap(checkIn, checkOut, r.getCheckIn(), r.getCheckOut()));
    }

    private boolean datesOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    private void sendConfirmationEmail(Reservation reservation) {
        Document document = generateDocument(reservation.getId());
        String documentText = formatDocument(document);

        String body = String.format(
                "Olá, %s!\n\n" +
                        "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
                        "%s\n\n" +
                        "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
                        "Até breve!",
                reservation.getGuestName(),
                documentText
        );

        messageService.sendEmail(
                reservation.getGuestEmail(),
                "Reserva confirmada - Pousada Maré Alta",
                body
        );
    }

    private String formatDocument(Document doc) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s nº %d\n", doc.getTitle(), doc.getReservationId()));
        sb.append(String.format("Hóspede: %s (CPF %s)\n",
                doc.getGuestName(),
                Formatter.formatCpf(doc.getGuestCpf())));
        sb.append(String.format("Quarto: %s (%s)\n",
                doc.getRoomNumber(),
                doc.getRoomType()));
        sb.append(String.format("Entrada: %s | Saída: %s | %d %s\n",
                Formatter.formatDate(doc.getCheckIn()),
                Formatter.formatDate(doc.getCheckOut()),
                doc.getNights(),
                doc.getNights() == 1 ? "diária" : "diárias"));
        sb.append(String.format("Diárias: %s\n", Formatter.formatCurrency(doc.getSubtotal())));

        if (doc.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("Desconto: %s\n", Formatter.formatCurrency(doc.getDiscount())));
        }

        sb.append(String.format("Total: %s", Formatter.formatCurrency(doc.getTotal())));

        return sb.toString();
    }

    public static class RoomNotFoundException extends RuntimeException {
        public RoomNotFoundException(String message) {
            super(message);
        }
    }

    public static class ReservationNotFoundException extends RuntimeException {
        public ReservationNotFoundException(String message) {
            super(message);
        }
    }

    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }

    public static class RoomUnavailableException extends RuntimeException {
        public RoomUnavailableException(String message) {
            super(message);
        }
    }

    public static class OperationNotAllowedException extends RuntimeException {
        public OperationNotAllowedException(String message) {
            super(message);
        }
    }

    public static class CapacityExceededException extends RuntimeException {
        public CapacityExceededException(String message) {
            super(message);
        }
    }
}
