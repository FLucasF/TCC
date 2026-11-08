package com.pousadamarealta.domain.service;

import com.pousadamarealta.domain.event.EventPublisher;
import com.pousadamarealta.domain.event.ReservationCancelledEvent;
import com.pousadamarealta.domain.event.ReservationConfirmedEvent;
import com.pousadamarealta.domain.model.*;
import com.pousadamarealta.repository.ReservationRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ReservationService {
    private static final Locale LOCALE_BR = new Locale.Builder().setLanguage("pt").setRegion("BR").build();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReservationRepository reservationRepository;
    private final RoomService roomService;
    private final EventPublisher eventPublisher;

    public ReservationService(ReservationRepository reservationRepository,
                            RoomService roomService,
                            EventPublisher eventPublisher) {
        this.reservationRepository = reservationRepository;
        this.roomService = roomService;
        this.eventPublisher = eventPublisher;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                        String guestCpf, String checkIn, String checkOut,
                                        int guests, RatePlan ratePlan) throws SQLException {
        Room room = roomService.getRoomByNumber(roomNumber);

        java.time.LocalDate checkInDate = java.time.LocalDate.parse(checkIn);
        java.time.LocalDate checkOutDate = java.time.LocalDate.parse(checkOut);

        if (guests < 1) {
            throw new ValidationException("At least one guest is required");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException("Room capacity exceeded");
        }

        if (!checkOutDate.isAfter(checkInDate)) {
            throw new ValidationException("Check-out must be after check-in");
        }

        List<Reservation> conflicts = reservationRepository.findConflicting(roomNumber, checkInDate, checkOutDate);
        if (!conflicts.isEmpty()) {
            throw new RoomUnavailableException("Room is not available for the selected dates");
        }

        Reservation reservation = new Reservation(
            roomNumber, guestName, guestEmail, guestCpf,
            checkInDate, checkOutDate, guests, ratePlan
        );

        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(Long id) throws SQLException {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));
    }

    public Reservation confirmReservation(Long id) throws SQLException {
        Reservation reservation = getReservation(id);

        if (!reservation.canConfirm()) {
            throw new OperationNotAllowedException("Cannot confirm reservation in status " + reservation.getStatus());
        }

        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
        BigDecimal total = reservation.calculateTotal(room.getNightlyRate());

        reservation.confirmWithTotal(total);
        reservationRepository.save(reservation);

        String receiptText = generateReceiptText(reservation, room.getType());
        eventPublisher.publishConfirmed(new ReservationConfirmedEvent(reservation, receiptText));

        return reservation;
    }

    public Reservation cancelReservation(Long id) throws SQLException {
        Reservation reservation = getReservation(id);

        if (!reservation.canCancel()) {
            throw new OperationNotAllowedException("Cannot cancel reservation in status " + reservation.getStatus());
        }

        boolean wasConfirmed = reservation.getStatus() == ReservationStatus.CONFIRMED;

        reservation.cancel();
        reservationRepository.save(reservation);

        eventPublisher.publishCancelled(new ReservationCancelledEvent(reservation, wasConfirmed));

        return reservation;
    }

    public Reservation checkIn(Long id) throws SQLException {
        Reservation reservation = getReservation(id);

        if (!reservation.canCheckIn()) {
            throw new OperationNotAllowedException("Cannot check in reservation in status " + reservation.getStatus());
        }

        reservation.checkIn();
        reservationRepository.save(reservation);

        return reservation;
    }

    public Reservation checkOut(Long id) throws SQLException {
        Reservation reservation = getReservation(id);

        if (!reservation.canCheckOut()) {
            throw new OperationNotAllowedException("Cannot check out reservation in status " + reservation.getStatus());
        }

        reservation.checkOut();
        reservationRepository.save(reservation);

        return reservation;
    }

    public String generateReceiptText(Reservation reservation, RoomType roomType) throws SQLException {
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
        BigDecimal subtotal = reservation.calculateSubtotal(room.getNightlyRate());
        BigDecimal discount = reservation.calculateDiscount(subtotal);
        BigDecimal total = subtotal.subtract(discount);

        StringBuilder receipt = new StringBuilder();
        receipt.append(String.format("Comprovante de reserva nº %d\n", reservation.getId()));
        receipt.append(String.format("Hóspede: %s (CPF %s)\n", reservation.getGuestName(), formatCpf(reservation.getGuestCpf())));
        receipt.append(String.format("Quarto: %s (%s)\n", reservation.getRoomNumber(), roomType.name()));
        receipt.append(String.format("Entrada: %s | Saída: %s | %d %s\n",
            reservation.getCheckIn().format(DATE_FORMATTER),
            reservation.getCheckOut().format(DATE_FORMATTER),
            reservation.getNights(),
            reservation.getNights() == 1 ? "diária" : "diárias"));
        receipt.append(String.format("Diárias: %s\n", formatMoney(subtotal)));

        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            receipt.append(String.format("Desconto: %s\n", formatMoney(discount)));
        }

        receipt.append(String.format("Total: %s", formatMoney(total)));

        return receipt.toString();
    }

    private String formatMoney(BigDecimal amount) {
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(LOCALE_BR);
        return currencyFormatter.format(amount);
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }

    public static class ReservationNotFoundException extends RuntimeException {
        public ReservationNotFoundException(String message) {
            super(message);
        }
    }

    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }

    public static class CapacityExceededException extends RuntimeException {
        public CapacityExceededException(String message) {
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
}
