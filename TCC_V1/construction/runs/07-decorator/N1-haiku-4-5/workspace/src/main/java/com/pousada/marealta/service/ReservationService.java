package com.pousada.marealta.service;

import com.pousada.marealta.domain.*;
import com.pousada.marealta.infrastructure.persistence.ReservationRepository;
import com.pousada.marealta.infrastructure.persistence.RoomRepository;
import com.pousada.marealta.infrastructure.messaging.MessageSender;
import com.pousada.marealta.util.DateFormatter;
import com.pousada.marealta.util.MoneyFormatter;

import java.time.LocalDate;
import java.util.List;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final MessageSender messageSender;

    public ReservationService(ReservationRepository reservationRepository,
                              RoomRepository roomRepository,
                              MessageSender messageSender) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.messageSender = messageSender;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                         LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) throws Exception {
        Room room = roomRepository.findByNumber(roomNumber);
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }

        if (guests < 1 || guests > room.getCapacity()) {
            throw new IllegalArgumentException("Invalid guest count");
        }

        if (checkOut.isBefore(checkIn) || checkOut.equals(checkIn)) {
            throw new IllegalArgumentException("Invalid dates");
        }

        List<Reservation> conflicts = reservationRepository.findByRoomAndDateRange(roomNumber, checkIn, checkOut);
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException("Room unavailable");
        }

        PriceCalculator.PriceBreakdown prices = PriceCalculator.calculate(
            room.getNightlyRate(), checkIn, checkOut, ratePlan);

        Reservation reservation = new Reservation(
            0, roomNumber, guestName, guestEmail, guestCpf,
            checkIn, checkOut, guests, ratePlan,
            prices.nights, prices.subtotal, prices.discount, prices.total,
            ReservationStatus.PENDING, 0, null
        );

        return reservationRepository.save(reservation);
    }

    public Reservation confirmReservation(long reservationId) throws Exception {
        Reservation reservation = reservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation not found");
        }

        if (!reservation.canBeConfirmed()) {
            throw new IllegalStateException("Reservation cannot be confirmed");
        }

        Reservation confirmed = new Reservation(
            reservation.getId(), reservation.getRoomNumber(), reservation.getGuestName(),
            reservation.getGuestEmail(), reservation.getGuestCpf(),
            reservation.getCheckIn(), reservation.getCheckOut(), reservation.getGuests(),
            reservation.getRatePlan(), reservation.getNights(), reservation.getSubtotal(),
            reservation.getDiscount(), reservation.getTotal(),
            ReservationStatus.CONFIRMED, reservation.getTotal(), null
        );

        Reservation saved = reservationRepository.save(confirmed);

        String receipt = generateReceipt(saved);
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = formatConfirmationEmail(saved.getGuestName(), receipt);

        messageSender.send(saved.getGuestEmail(), subject, body);

        return saved;
    }

    public Reservation cancelReservation(long reservationId) throws Exception {
        Reservation reservation = reservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation not found");
        }

        if (!reservation.canBeCancelled()) {
            throw new IllegalStateException("Reservation cannot be cancelled");
        }

        Double refund = null;
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            if (reservation.getRatePlan() == RatePlan.STANDARD) {
                refund = reservation.getAmountPaid();
            } else {
                refund = 0.0;
            }
        }

        Reservation cancelled = new Reservation(
            reservation.getId(), reservation.getRoomNumber(), reservation.getGuestName(),
            reservation.getGuestEmail(), reservation.getGuestCpf(),
            reservation.getCheckIn(), reservation.getCheckOut(), reservation.getGuests(),
            reservation.getRatePlan(), reservation.getNights(), reservation.getSubtotal(),
            reservation.getDiscount(), reservation.getTotal(),
            ReservationStatus.CANCELLED, reservation.getAmountPaid(), refund
        );

        return reservationRepository.save(cancelled);
    }

    public Reservation checkIn(long reservationId) throws Exception {
        Reservation reservation = reservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation not found");
        }

        if (!reservation.canBeCheckedIn()) {
            throw new IllegalStateException("Reservation cannot be checked in");
        }

        Reservation checkedIn = new Reservation(
            reservation.getId(), reservation.getRoomNumber(), reservation.getGuestName(),
            reservation.getGuestEmail(), reservation.getGuestCpf(),
            reservation.getCheckIn(), reservation.getCheckOut(), reservation.getGuests(),
            reservation.getRatePlan(), reservation.getNights(), reservation.getSubtotal(),
            reservation.getDiscount(), reservation.getTotal(),
            ReservationStatus.CHECKED_IN, reservation.getAmountPaid(), reservation.getRefund()
        );

        return reservationRepository.save(checkedIn);
    }

    public Reservation checkOut(long reservationId) throws Exception {
        Reservation reservation = reservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation not found");
        }

        if (!reservation.canBeCheckedOut()) {
            throw new IllegalStateException("Reservation cannot be checked out");
        }

        Reservation checkedOut = new Reservation(
            reservation.getId(), reservation.getRoomNumber(), reservation.getGuestName(),
            reservation.getGuestEmail(), reservation.getGuestCpf(),
            reservation.getCheckIn(), reservation.getCheckOut(), reservation.getGuests(),
            reservation.getRatePlan(), reservation.getNights(), reservation.getSubtotal(),
            reservation.getDiscount(), reservation.getTotal(),
            ReservationStatus.CHECKED_OUT, reservation.getAmountPaid(), reservation.getRefund()
        );

        return reservationRepository.save(checkedOut);
    }

    public String generateReceipt(Reservation reservation) throws Exception {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber());
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }

        StringBuilder receipt = new StringBuilder();
        receipt.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        receipt.append("Hóspede: ").append(reservation.getGuestName())
            .append(" (CPF ").append(reservation.getGuestCpf()).append(")\n");
        receipt.append("Quarto: ").append(reservation.getRoomNumber())
            .append(" (").append(room.getType().getName()).append(")\n");
        receipt.append("Entrada: ").append(DateFormatter.toBrazilian(reservation.getCheckIn()))
            .append(" | Saída: ").append(DateFormatter.toBrazilian(reservation.getCheckOut()))
            .append(" | ").append(reservation.getNights()).append(" diária").append(reservation.getNights() != 1 ? "s" : "").append("\n");
        receipt.append("Diárias: ").append(MoneyFormatter.formatForDisplay(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount() > 0) {
            receipt.append("Desconto: ").append(MoneyFormatter.formatForDisplay(reservation.getDiscount())).append("\n");
        }

        receipt.append("Total: ").append(MoneyFormatter.formatForDisplay(reservation.getTotal())).append("\n");

        return receipt.toString();
    }

    private String formatConfirmationEmail(String guestName, String receipt) {
        return "Olá, " + guestName + "!\n\n" +
                "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
                receipt + "\n" +
                "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
                "Até breve!";
    }
}
