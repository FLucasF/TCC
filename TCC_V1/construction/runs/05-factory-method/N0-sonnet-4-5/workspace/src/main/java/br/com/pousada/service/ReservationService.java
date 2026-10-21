package br.com.pousada.service;

import br.com.pousada.model.*;
import br.com.pousada.util.PriceCalculator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class ReservationService {

    private final Map<Long, Reservation> reservations;
    private final AtomicLong reservationIdCounter;
    private final RoomService roomService;
    private final MessageService messageService;
    private final DocumentService documentService;

    public ReservationService(RoomService roomService, MessageService messageService, DocumentService documentService) {
        this.reservations = new HashMap<>();
        this.reservationIdCounter = new AtomicLong(1);
        this.roomService = roomService;
        this.messageService = messageService;
        this.documentService = documentService;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                        String guestCpf, String companyCnpj, String companyName,
                                        String guestPassport, String guestCountry,
                                        LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        Room room = roomService.getRoomByNumber(roomNumber)
            .orElseThrow(() -> new RoomNotFoundException());

        if (guests < 1) {
            throw new InvalidRequestException("Invalid number of guests");
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

        int nights = (int) checkIn.until(checkOut).getDays();
        BigDecimal subtotal = PriceCalculator.calculateSubtotal(room.getNightlyRate(), checkIn, checkOut);
        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = PriceCalculator.calculateTotal(subtotal, discount);

        Long id = reservationIdCounter.getAndIncrement();
        Reservation reservation = new Reservation(
            id, roomNumber, guestName, guestEmail,
            guestCpf, companyCnpj, companyName, guestPassport, guestCountry,
            checkIn, checkOut, guests, ratePlan,
            nights, subtotal, discount, total
        );

        reservations.put(id, reservation);
        return reservation;
    }

    public Optional<Reservation> getReservation(Long id) {
        return Optional.ofNullable(reservations.get(id));
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = getReservation(id)
            .orElseThrow(() -> new ReservationNotFoundException());

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());

        sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = getReservation(id)
            .orElseThrow(() -> new ReservationNotFoundException());

        if (reservation.getStatus() != ReservationStatus.PENDING &&
            reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException();
        }

        BigDecimal refundAmount = BigDecimal.ZERO;
        if (reservation.getStatus() == ReservationStatus.CONFIRMED &&
            reservation.getRatePlan() == RatePlan.STANDARD) {
            refundAmount = reservation.getAmountPaid();
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setRefund(refundAmount);

        return reservation;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = getReservation(id)
            .orElseThrow(() -> new ReservationNotFoundException());

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        return reservation;
    }

    public Document checkOut(Long id) {
        Reservation reservation = getReservation(id)
            .orElseThrow(() -> new ReservationNotFoundException());

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);

        Room room = roomService.getRoomByNumber(reservation.getRoomNumber()).get();
        return documentService.generateDocument(reservation, room);
    }

    public Document getDocument(Long id) {
        Reservation reservation = getReservation(id)
            .orElseThrow(() -> new ReservationNotFoundException());

        if (reservation.getStatus() != ReservationStatus.CONFIRMED &&
            reservation.getStatus() != ReservationStatus.CHECKED_IN &&
            reservation.getStatus() != ReservationStatus.CHECKED_OUT) {
            throw new OperationNotAllowedException();
        }

        Room room = roomService.getRoomByNumber(reservation.getRoomNumber()).get();
        return documentService.generateDocument(reservation, room);
    }

    private void sendConfirmationEmail(Reservation reservation) {
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber()).get();
        Document document = documentService.generateDocument(reservation, room);

        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = String.format(
            "Olá, %s!\n\n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
            "%s\n\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!",
            reservation.getGuestName(),
            document.getText()
        );

        messageService.sendEmail(reservation.getGuestEmail(), subject, body);
    }

    private boolean isRoomUnavailable(String roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeReservationId) {
        return reservations.values().stream()
            .filter(r -> !r.getId().equals(excludeReservationId))
            .filter(r -> r.getRoomNumber().equals(roomNumber))
            .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
            .anyMatch(r -> datesOverlap(checkIn, checkOut, r.getCheckIn(), r.getCheckOut()));
    }

    private boolean datesOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    public List<Reservation> getAllReservations() {
        return List.copyOf(reservations.values());
    }

    public static class RoomNotFoundException extends RuntimeException {
    }

    public static class ReservationNotFoundException extends RuntimeException {
    }

    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }

    public static class RoomUnavailableException extends RuntimeException {
    }

    public static class CapacityExceededException extends RuntimeException {
    }

    public static class OperationNotAllowedException extends RuntimeException {
    }
}
