package com.pousadamarealta.service;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.messaging.PendingMessage;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final MessageService messageService;

    public ReservationService(ReservationRepository reservationRepository,
                              RoomRepository roomRepository,
                              MessageService messageService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.messageService = messageService;
    }

    public Reservation create(String roomNumber, String guestName, String guestEmail, String guestCpf,
                              LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        Room room = roomRepository.findByNumber(roomNumber)
                .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomNumber));

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException("Room capacity exceeded");
        }

        List<Reservation> conflicts = reservationRepository.findByRoomAndDateRange(roomNumber, checkIn, checkOut);
        if (!conflicts.isEmpty()) {
            throw new RoomUnavailableException("Room is not available for the selected dates");
        }

        Reservation reservation = new Reservation(
                roomNumber, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan, room.getNightlyRate()
        );

        return reservationRepository.save(reservation);
    }

    public Reservation findById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found: " + id));
    }

    public Reservation confirm(Long id) {
        Reservation reservation = findById(id);

        try {
            reservation.confirm();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }

        reservationRepository.update(reservation);
        sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation cancel(Long id) {
        Reservation reservation = findById(id);

        try {
            reservation.cancel();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }

        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = findById(id);

        try {
            reservation.checkIn();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }

        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = findById(id);

        try {
            reservation.checkOut();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }

        reservationRepository.update(reservation);

        return reservation;
    }

    public String generateDocument(Reservation reservation) {
        if (!reservation.isPaid()) {
            throw new OperationNotAllowedException("Document is only available for paid reservations");
        }

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();

        StringBuilder doc = new StringBuilder();
        doc.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        doc.append("Hóspede: ").append(reservation.getGuestName())
                .append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        doc.append("Quarto: ").append(reservation.getRoomNumber())
                .append(" (").append(room.getType()).append(")\n");
        doc.append("Entrada: ").append(formatDate(reservation.getCheckIn()))
                .append(" | Saída: ").append(formatDate(reservation.getCheckOut()))
                .append(" | ").append(reservation.getNights())
                .append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        doc.append("Diárias: ").append(formatMoney(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            doc.append("Desconto: ").append(formatMoney(reservation.getDiscount())).append("\n");
        }

        doc.append("Total: ").append(formatMoney(reservation.getTotal()));

        return doc.toString();
    }

    private void sendConfirmationEmail(Reservation reservation) {
        String document = generateDocument(reservation);

        String body = String.format("""
                Olá, %s!

                Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

                %s

                A entrada é a partir das 14h, e a saída, até as 12h.
                Até breve!""", reservation.getGuestName(), document);

        PendingMessage message = new PendingMessage(
                MessageChannel.EMAIL,
                reservation.getGuestEmail(),
                "Reserva confirmada - Pousada Maré Alta",
                body
        );

        messageService.send(message);
    }

    private String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String formatMoney(BigDecimal amount) {
        return String.format(Locale.forLanguageTag("pt-BR"), "R$ %,.2f", amount);
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                    cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
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

    public static class RoomUnavailableException extends RuntimeException {
        public RoomUnavailableException(String message) {
            super(message);
        }
    }

    public static class CapacityExceededException extends RuntimeException {
        public CapacityExceededException(String message) {
            super(message);
        }
    }

    public static class OperationNotAllowedException extends RuntimeException {
        public OperationNotAllowedException(String message) {
            super(message);
        }
    }
}
