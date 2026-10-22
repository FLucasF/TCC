package br.com.marealta.service;

import br.com.marealta.domain.*;
import br.com.marealta.messaging.Message;
import br.com.marealta.messaging.MessageSender;
import br.com.marealta.persistence.ReservationRepository;

import java.time.LocalDate;

public class ReservationService {
    public record NewReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                 LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {}

    private final ReservationRepository reservations;
    private final MessageSender sender;

    public ReservationService(ReservationRepository reservations, MessageSender sender) {
        this.reservations = reservations;
        this.sender = sender;
    }

    public Reservation create(NewReservation req) {
        Room room = RoomCatalog.find(req.roomNumber()).orElseThrow(() -> ApiException.roomNotFound(req.roomNumber()));
        if (req.guests() > room.capacity()) throw ApiException.capacityExceeded(room.capacity());
        Pricing.Quote quote = Pricing.quote(room, req.checkIn(), req.checkOut(), req.ratePlan());
        return reservations.insert(new Reservation(0, room.number(), req.guestName(), req.guestEmail(),
                req.guestCpf(), req.checkIn(), req.checkOut(), req.guests(), req.ratePlan(), quote,
                ReservationStatus.PENDING, Money.ZERO, null));
    }

    public Reservation get(long id) {
        return reservations.find(id).orElseThrow(() -> ApiException.reservationNotFound(id));
    }

    public Reservation confirm(long id) {
        Reservation r = reservations.modify(id, Reservation::confirm);
        sender.send(Message.email(r.guestEmail(), "Reserva confirmada - Pousada Maré Alta", confirmationBody(r)));
        return r;
    }

    public Reservation cancel(long id) {
        return reservations.modify(id, Reservation::cancel);
    }

    public Reservation checkIn(long id) {
        return reservations.modify(id, Reservation::checkIn);
    }

    public Receipt checkOut(long id) {
        return receiptOf(reservations.modify(id, Reservation::checkOut));
    }

    public Receipt document(long id) {
        Reservation r = get(id);
        r.requirePaid();
        return receiptOf(r);
    }

    private Receipt receiptOf(Reservation r) {
        return Receipt.of(r, RoomCatalog.find(r.roomNumber()).orElseThrow());
    }

    private String confirmationBody(Reservation r) {
        return "Olá, " + r.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + receiptOf(r).text() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
    }
}
