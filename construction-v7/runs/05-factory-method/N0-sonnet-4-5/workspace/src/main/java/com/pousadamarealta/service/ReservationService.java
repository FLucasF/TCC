package com.pousadamarealta.service;

import com.pousadamarealta.model.*;
import com.pousadamarealta.util.DocumentFormatter;
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
                                        GuestIdentification identification, LocalDate checkIn,
                                        LocalDate checkOut, int guests, RatePlan ratePlan) {
        Room room = roomService.getRoomByNumber(roomNumber)
            .orElseThrow(() -> new ServiceException("ROOM_NOT_FOUND", "Room not found", 404));

        if (guests > room.getCapacity()) {
            throw new ServiceException("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity", 422);
        }

        if (guests < 1) {
            throw new ServiceException("INVALID_REQUEST", "Number of guests must be at least 1", 400);
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new ServiceException("INVALID_REQUEST", "Check-out must be after check-in", 400);
        }

        if (isRoomUnavailable(roomNumber, checkIn, checkOut, null)) {
            throw new ServiceException("ROOM_UNAVAILABLE", "Room is not available for the selected dates", 409);
        }

        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal subtotal = PriceCalculator.calculateSubtotal(room, checkIn, checkOut);
        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = PriceCalculator.round(subtotal.subtract(discount));

        Reservation reservation = new Reservation(
            nextId.getAndIncrement(),
            roomNumber,
            guestName,
            guestEmail,
            identification,
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

    public Optional<Reservation> getReservation(long id) {
        return Optional.ofNullable(reservations.get(id));
    }

    public Reservation confirmReservation(long id) {
        Reservation reservation = getReservation(id)
            .orElseThrow(() -> new ServiceException("RESERVATION_NOT_FOUND", "Reservation not found", 404));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ServiceException("OPERATION_NOT_ALLOWED",
                "Can only confirm pending reservations", 409);
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());

        sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation cancelReservation(long id) {
        Reservation reservation = getReservation(id)
            .orElseThrow(() -> new ServiceException("RESERVATION_NOT_FOUND", "Reservation not found", 404));

        if (reservation.getStatus() == ReservationStatus.CHECKED_IN ||
            reservation.getStatus() == ReservationStatus.CHECKED_OUT ||
            reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ServiceException("OPERATION_NOT_ALLOWED",
                "Cannot cancel reservation in current status", 409);
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        if (reservation.getRatePlan() == RatePlan.STANDARD) {
            reservation.setRefund(reservation.getAmountPaid());
        } else {
            reservation.setRefund(BigDecimal.ZERO);
        }

        return reservation;
    }

    public Reservation checkIn(long id) {
        Reservation reservation = getReservation(id)
            .orElseThrow(() -> new ServiceException("RESERVATION_NOT_FOUND", "Reservation not found", 404));

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ServiceException("OPERATION_NOT_ALLOWED",
                "Can only check-in confirmed reservations", 409);
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        return reservation;
    }

    public Document checkOut(long id) {
        Reservation reservation = getReservation(id)
            .orElseThrow(() -> new ServiceException("RESERVATION_NOT_FOUND", "Reservation not found", 404));

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new ServiceException("OPERATION_NOT_ALLOWED",
                "Can only check-out reservations that are checked in", 409);
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        return generateDocument(reservation);
    }

    public Document getDocument(long id) {
        Reservation reservation = getReservation(id)
            .orElseThrow(() -> new ServiceException("RESERVATION_NOT_FOUND", "Reservation not found", 404));

        if (reservation.getStatus() == ReservationStatus.PENDING) {
            throw new ServiceException("OPERATION_NOT_ALLOWED",
                "Document only available for paid reservations", 409);
        }

        return generateDocument(reservation);
    }

    private Document generateDocument(Reservation reservation) {
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber()).orElseThrow();
        GuestIdentification id = reservation.getIdentification();

        String title;
        String text;

        if (id.isCpf()) {
            title = "Comprovante de reserva";
            text = DocumentFormatter.formatReceipt(reservation, room);
        } else if (id.isCompany()) {
            title = "Fatura de hospedagem";
            text = DocumentFormatter.formatInvoice(reservation, room);
        } else {
            title = "Booking receipt";
            text = DocumentFormatter.formatBookingReceipt(reservation, room);
        }

        return new Document(
            title,
            reservation.getId(),
            reservation.getGuestName(),
            id.getGuestCpf(),
            id.getCompanyCnpj(),
            id.getCompanyName(),
            id.getGuestPassport(),
            id.getGuestCountry(),
            reservation.getRoomNumber(),
            room.getType(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal(),
            text
        );
    }

    private void sendConfirmationEmail(Reservation reservation) {
        Document document = generateDocument(reservation);
        String emailBody = DocumentFormatter.formatConfirmationEmail(
            reservation.getGuestName(),
            document.getText()
        );

        messageService.sendEmail(
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            emailBody
        );
    }

    private boolean isRoomUnavailable(String roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeReservationId) {
        return reservations.values().stream()
            .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
            .filter(r -> excludeReservationId == null || r.getId() != excludeReservationId)
            .filter(r -> r.getRoomNumber().equals(roomNumber))
            .anyMatch(r -> datesOverlap(checkIn, checkOut, r.getCheckIn(), r.getCheckOut()));
    }

    private boolean datesOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    public List<Reservation> getAllReservations() {
        return List.copyOf(reservations.values());
    }
}
