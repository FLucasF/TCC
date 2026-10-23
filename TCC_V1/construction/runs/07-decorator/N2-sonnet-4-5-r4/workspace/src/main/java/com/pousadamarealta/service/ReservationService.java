package com.pousadamarealta.service;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.messaging.MessageProcessor;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final MessageRepository messageRepository;
    private final RoomService roomService;
    private final MessageProcessor messageProcessor;

    public ReservationService(ReservationRepository reservationRepository,
                            MessageRepository messageRepository,
                            RoomService roomService,
                            MessageProcessor messageProcessor) {
        this.reservationRepository = reservationRepository;
        this.messageRepository = messageRepository;
        this.roomService = roomService;
        this.messageProcessor = messageProcessor;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlan ratePlan) {
        Room room = roomService.getRoomByNumber(roomNumber);

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException();
        }

        if (reservationRepository.hasConflict(roomNumber, checkIn, checkOut, null)) {
            throw new RoomUnavailableException();
        }

        Reservation reservation = new Reservation(
            roomNumber, guestName, guestEmail, guestCpf,
            checkIn, checkOut, guests, ratePlan
        );

        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException(id));
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = getReservation(id);
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());

        try {
            reservation.confirm(room.getNightlyRate());
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }

        Reservation saved = reservationRepository.save(reservation);
        sendConfirmationEmail(saved, room);

        return saved;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = getReservation(id);

        try {
            reservation.checkIn();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }

        return reservationRepository.save(reservation);
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = getReservation(id);

        try {
            reservation.checkOut();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }

        return reservationRepository.save(reservation);
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = getReservation(id);

        try {
            reservation.cancel();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }

        return reservationRepository.save(reservation);
    }

    public String generateDocument(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() == ReservationStatus.PENDING) {
            throw new OperationNotAllowedException("Document only available for confirmed reservations");
        }

        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
        return formatDocument(reservation, room);
    }

    private void sendConfirmationEmail(Reservation reservation, Room room) {
        String document = formatDocument(reservation, room);

        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!
            """, reservation.getGuestName(), document);

        Message message = new Message(
            MessageChannel.EMAIL,
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            body
        );

        List<Message> processedMessages = messageProcessor.process(message);

        for (Message msg : processedMessages) {
            messageRepository.save(msg);
        }
    }

    private String formatDocument(Reservation reservation, Room room) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        BigDecimal subtotal = reservation.getSubtotal(room.getNightlyRate());
        BigDecimal discount = reservation.getDiscount(subtotal);
        BigDecimal total = reservation.getTotal(room.getNightlyRate());

        int nights = reservation.getNights();
        String nightsText = nights == 1 ? "1 diária" : nights + " diárias";

        StringBuilder doc = new StringBuilder();
        doc.append(String.format("Comprovante de reserva nº %d\n", reservation.getId()));
        doc.append(String.format("Hóspede: %s (CPF %s)\n",
            reservation.getGuestName(), formatCpf(reservation.getGuestCpf())));
        doc.append(String.format("Quarto: %s (%s)\n",
            reservation.getRoomNumber(), room.getType().name()));
        doc.append(String.format("Entrada: %s | Saída: %s | %s\n",
            reservation.getCheckIn().format(dateFormatter),
            reservation.getCheckOut().format(dateFormatter),
            nightsText));
        doc.append(String.format("Diárias: %s\n", formatMoney(subtotal)));

        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            doc.append(String.format("Desconto: %s\n", formatMoney(discount)));
        }

        doc.append(String.format("Total: %s", formatMoney(total)));

        return doc.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("R$ %,.2f", amount).replace(",", "X").replace(".", ",").replace("X", ".");
    }

    public List<Reservation> getReservationsByNight(LocalDate night) {
        return reservationRepository.findByNight(night);
    }

    public List<Reservation> getReservationsByMonth(YearMonth month) {
        return reservationRepository.findByMonth(month);
    }
}
