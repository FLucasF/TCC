package com.pousadamarealta.service;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;
    private final Map<Class<? extends GuestIdentification>, DocumentGenerator> documentGenerators;

    public ReservationService(ReservationRepository reservationRepository,
                            RoomRepository roomRepository,
                            MessageRepository messageRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.messageRepository = messageRepository;
        this.documentGenerators = new HashMap<>();
        this.documentGenerators.put(CpfIdentification.class, new CpfDocumentGenerator());
        this.documentGenerators.put(CnpjIdentification.class, new CnpjDocumentGenerator());
        this.documentGenerators.put(PassportIdentification.class, new PassportDocumentGenerator());
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                       GuestIdentification identification,
                                       LocalDate checkIn, LocalDate checkOut,
                                       int guests, RatePlan ratePlan) {
        Room room = roomRepository.findByNumber(roomNumber)
            .orElseThrow(() -> new RoomNotFoundException());

        if (guests < 1) {
            throw new InvalidRequestException("Invalid number of guests");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException();
        }

        if (reservationRepository.hasConflict(roomNumber, checkIn, checkOut, null)) {
            throw new RoomUnavailableException();
        }

        long id = reservationRepository.nextId();
        Reservation reservation = new Reservation(
            id, roomNumber, guestName, guestEmail, identification,
            checkIn, checkOut, guests, ratePlan
        );

        return reservationRepository.save(reservation);
    }

    public Reservation confirmReservation(long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());

        if (!reservation.canConfirm()) {
            throw new OperationNotAllowedException();
        }

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).get();
        BigDecimal subtotal = reservation.getSubtotal(room.getNightlyRate());
        BigDecimal discount = reservation.getDiscount(subtotal);
        BigDecimal total = reservation.getTotal(subtotal, discount);

        reservation.confirm(total);

        Document document = generateDocument(reservation, room, subtotal, discount, total);
        sendConfirmationEmail(reservation, document);

        return reservation;
    }

    public Reservation cancelReservation(long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());

        if (!reservation.canCancel()) {
            throw new OperationNotAllowedException();
        }

        reservation.cancel();
        return reservation;
    }

    public Reservation checkIn(long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());

        if (!reservation.canCheckIn()) {
            throw new OperationNotAllowedException();
        }

        reservation.checkIn();
        return reservation;
    }

    public Document checkOut(long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());

        if (!reservation.canCheckOut()) {
            throw new OperationNotAllowedException();
        }

        reservation.checkOut();

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).get();
        BigDecimal subtotal = reservation.getSubtotal(room.getNightlyRate());
        BigDecimal discount = reservation.getDiscount(subtotal);
        BigDecimal total = reservation.getTotal(subtotal, discount);

        return generateDocument(reservation, room, subtotal, discount, total);
    }

    public Document getDocument(long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());

        if (!reservation.isPaid()) {
            throw new OperationNotAllowedException();
        }

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).get();
        BigDecimal subtotal = reservation.getSubtotal(room.getNightlyRate());
        BigDecimal discount = reservation.getDiscount(subtotal);
        BigDecimal total = reservation.getTotal(subtotal, discount);

        return generateDocument(reservation, room, subtotal, discount, total);
    }

    private Document generateDocument(Reservation reservation, Room room,
                                     BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
        DocumentGenerator generator = documentGenerators.get(reservation.getIdentification().getClass());
        return generator.generate(reservation, room, subtotal, discount, total);
    }

    private void sendConfirmationEmail(Reservation reservation, Document document) {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = "Olá, " + reservation.getGuestName() + "!\n\n" +
                     "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
                     document.getText() + "\n\n" +
                     "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
                     "Até breve!";

        long messageId = messageRepository.nextId();
        Message message = new Message(messageId, "EMAIL", reservation.getGuestEmail(), subject, body);
        messageRepository.save(message);
    }
}
