package com.pousada.service;

import com.pousada.domain.*;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

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

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlan ratePlan) {
        Room room = roomRepository.findByNumber(roomNumber)
            .orElseThrow(() -> new RoomNotFoundException(roomNumber));

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException(guests, room.getCapacity());
        }

        if (guests < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        List<Reservation> conflicts = reservationRepository.findByRoomAndDateRange(roomNumber, checkIn, checkOut);
        if (!conflicts.isEmpty()) {
            throw new RoomUnavailableException(roomNumber, checkIn, checkOut);
        }

        Reservation reservation = new Reservation(
            roomNumber, guestName, guestEmail, guestCpf,
            checkIn, checkOut, guests, ratePlan,
            room.getNightlyRate()
        );

        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException(id));
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.getStatus().canConfirm()) {
            throw new OperationNotAllowedException("Cannot confirm reservation in status " + reservation.getStatus());
        }

        reservation.confirm();
        reservationRepository.save(reservation);

        sendConfirmationEmail(reservation);

        return reservation;
    }

    private void sendConfirmationEmail(Reservation reservation) {
        ReservationDocument document = createDocument(reservation);
        String documentText = formatDocument(document);

        String emailBody = String.format(
            """
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!""",
            reservation.getGuestName(),
            documentText
        );

        messageService.send(
            MessageChannel.EMAIL,
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            emailBody
        );
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.getStatus().canCancel()) {
            throw new OperationNotAllowedException("Cannot cancel reservation in status " + reservation.getStatus());
        }

        reservation.cancel();
        return reservationRepository.save(reservation);
    }

    public Reservation checkInReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.getStatus().canCheckIn()) {
            throw new OperationNotAllowedException("Cannot check-in reservation in status " + reservation.getStatus());
        }

        reservation.checkIn();
        return reservationRepository.save(reservation);
    }

    public ReservationDocument checkOutReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.getStatus().canCheckOut()) {
            throw new OperationNotAllowedException("Cannot check-out reservation in status " + reservation.getStatus());
        }

        reservation.checkOut();
        reservationRepository.save(reservation);

        return createDocument(reservation);
    }

    public ReservationDocument getDocument(Long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.getStatus().isPaid()) {
            throw new OperationNotAllowedException("Document is only available for paid reservations");
        }

        return createDocument(reservation);
    }

    private ReservationDocument createDocument(Reservation reservation) {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();

        return new ReservationDocument(
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

    private String formatDocument(ReservationDocument doc) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        String cpf = formatCpf(doc.getGuestCpf());
        String checkIn = doc.getCheckIn().format(dateFormatter);
        String checkOut = doc.getCheckOut().format(dateFormatter);
        String nights = doc.getNights() == 1 ? "1 diária" : doc.getNights() + " diárias";
        String subtotal = formatMoney(doc.getSubtotal());

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Comprovante de reserva nº %d\n", doc.getReservationId()));
        sb.append(String.format("Hóspede: %s (CPF %s)\n", doc.getGuestName(), cpf));
        sb.append(String.format("Quarto: %s (%s)\n", doc.getRoomNumber(), doc.getRoomType()));
        sb.append(String.format("Entrada: %s | Saída: %s | %s\n", checkIn, checkOut, nights));
        sb.append(String.format("Diárias: %s\n", subtotal));

        if (doc.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("Desconto: %s\n", formatMoney(doc.getDiscount())));
        }

        sb.append(String.format("Total: %s", formatMoney(doc.getTotal())));

        return sb.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return String.format("%s.%s.%s-%s",
                cpf.substring(0, 3),
                cpf.substring(3, 6),
                cpf.substring(6, 9),
                cpf.substring(9, 11));
        }
        return cpf;
    }

    private String formatMoney(BigDecimal value) {
        return String.format("R$ %,.2f", value).replace(',', 'X').replace('.', ',').replace('X', '.');
    }

    public static class RoomNotFoundException extends RuntimeException {
        public RoomNotFoundException(String roomNumber) {
            super("Room not found: " + roomNumber);
        }
    }

    public static class ReservationNotFoundException extends RuntimeException {
        public ReservationNotFoundException(Long id) {
            super("Reservation not found: " + id);
        }
    }

    public static class RoomUnavailableException extends RuntimeException {
        public RoomUnavailableException(String roomNumber, LocalDate from, LocalDate to) {
            super(String.format("Room %s is not available from %s to %s", roomNumber, from, to));
        }
    }

    public static class CapacityExceededException extends RuntimeException {
        public CapacityExceededException(int requested, int capacity) {
            super(String.format("Requested %d guests exceeds room capacity of %d", requested, capacity));
        }
    }

    public static class OperationNotAllowedException extends RuntimeException {
        public OperationNotAllowedException(String message) {
            super(message);
        }
    }

    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }
}
