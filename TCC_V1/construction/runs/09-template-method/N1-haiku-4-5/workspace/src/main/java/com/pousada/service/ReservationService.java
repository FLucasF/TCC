package com.pousada.service;

import com.pousada.domain.*;
import com.pousada.util.PriceCalculator;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationService {
    private final ReservationRepository reservationRepo;
    private final RoomRepository roomRepo;
    private final MessageRepository messageRepo;

    public ReservationService(ReservationRepository reservationRepo, RoomRepository roomRepo, MessageRepository messageRepo) {
        this.reservationRepo = reservationRepo;
        this.roomRepo = roomRepo;
        this.messageRepo = messageRepo;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                        LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        Room room = roomRepo.getRoom(roomNumber);
        if (room == null) {
            return null;
        }

        if (guests < 1 || guests > room.getCapacity()) {
            throw new IllegalArgumentException("Guest count exceeds room capacity");
        }

        int nights = PriceCalculator.calculateNights(checkIn, checkOut);
        if (nights <= 0) {
            throw new IllegalArgumentException("Checkout must be after checkin");
        }

        BigDecimal subtotal = PriceCalculator.calculateSubtotal(room.getNightlyRate(), checkIn, checkOut);
        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, ratePlan.name());
        BigDecimal total = PriceCalculator.calculateTotal(subtotal, discount);

        long id = reservationRepo.getNextId();
        Reservation reservation = new Reservation(id, roomNumber, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan, nights, subtotal, discount, total);

        return reservationRepo.save(reservation);
    }

    public Reservation getReservation(long id) {
        return reservationRepo.getReservation(id);
    }

    public Reservation confirmReservation(long id, BigDecimal amount) {
        Reservation reservation = reservationRepo.getReservation(id);
        if (reservation == null || reservation.getStatus() != ReservationStatus.PENDING) {
            return null;
        }

        reservation.confirm(amount);

        String body = generateConfirmationEmail(reservation);
        Message message = new Message(messageRepo.getNextId(), "EMAIL", reservation.getGuestEmail(),
                "Reserva confirmada - Pousada Maré Alta", body);
        messageRepo.save(message);

        return reservation;
    }

    public Reservation cancelReservation(long id) {
        Reservation reservation = reservationRepo.getReservation(id);
        if (reservation == null) {
            return null;
        }

        ReservationStatus status = reservation.getStatus();
        if (status == ReservationStatus.CHECKED_IN || status == ReservationStatus.CHECKED_OUT || status == ReservationStatus.CANCELLED) {
            return null;
        }

        BigDecimal refundAmount = calculateRefund(reservation);
        reservation.cancel(refundAmount);

        return reservation;
    }

    public Reservation checkIn(long id) {
        Reservation reservation = reservationRepo.getReservation(id);
        if (reservation == null || reservation.getStatus() != ReservationStatus.CONFIRMED) {
            return null;
        }

        reservation.checkIn();
        return reservation;
    }

    public Reservation checkOut(long id) {
        Reservation reservation = reservationRepo.getReservation(id);
        if (reservation == null || reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            return null;
        }

        reservation.checkOut();
        return reservation;
    }

    private BigDecimal calculateRefund(Reservation reservation) {
        if (reservation.getStatus() == ReservationStatus.PENDING) {
            return PriceCalculator.round(BigDecimal.ZERO);
        }

        if (reservation.getRatePlan() == RatePlan.NON_REFUNDABLE) {
            return PriceCalculator.round(BigDecimal.ZERO);
        }

        return reservation.getAmountPaid();
    }

    private String generateConfirmationEmail(Reservation reservation) {
        String receipt = generateReceipt(reservation);
        return "Olá, " + reservation.getGuestName() + "!\n\n" +
               "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
               receipt + "\n" +
               "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
               "Até breve!";
    }

    public String generateReceipt(Reservation reservation) {
        Room room = roomRepo.getRoom(reservation.getRoomNumber());
        String nightsText = reservation.getNights() == 1 ? "1 diária" : reservation.getNights() + " diárias";

        String receipt = "Comprovante de reserva nº " + reservation.getId() + "\n" +
                "Hóspede: " + reservation.getGuestName() + " (CPF " + reservation.getGuestCpf() + ")\n" +
                "Quarto: " + room.getNumber() + " (" + room.getType().getDisplayName() + ")\n" +
                "Entrada: " + formatDate(reservation.getCheckIn()) + " | Saída: " + formatDate(reservation.getCheckOut()) + " | " + nightsText + "\n" +
                "Diárias: " + formatCurrency(reservation.getSubtotal());

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            receipt += "\nDesconto: " + formatCurrency(reservation.getDiscount());
        }

        receipt += "\nTotal: " + formatCurrency(reservation.getTotal());

        return receipt;
    }

    private String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private String formatCurrency(BigDecimal value) {
        return "R$ " + String.format("%.2f", value).replace(".", ",");
    }
}
