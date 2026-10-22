package com.pousada.service;

import com.pousada.domain.Document;
import com.pousada.domain.Message;
import com.pousada.domain.Money;
import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;
import com.pousada.domain.Room;
import com.pousada.error.ApiException;
import com.pousada.persistence.Database;
import com.pousada.persistence.MessageRepository;
import com.pousada.persistence.ReservationRepository;
import com.pousada.persistence.RoomRepository;
import com.pousada.plan.PriceQuote;
import com.pousada.plan.PricingRequest;
import com.pousada.plan.RatePlan;
import com.pousada.plan.RatePlans;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.function.UnaryOperator;

public class ReservationService {

    private final Database db;
    private final RoomRepository rooms;
    private final ReservationRepository reservations;
    private final MessageRepository messages;
    private final RatePlans plans;

    public ReservationService(Database db, RoomRepository rooms, ReservationRepository reservations,
                              MessageRepository messages, RatePlans plans) {
        this.db = db;
        this.rooms = rooms;
        this.reservations = reservations;
        this.messages = messages;
        this.plans = plans;
    }

    public List<Room> listRooms() {
        return db.tx(rooms::findAll);
    }

    public List<Message> listMessages() {
        return db.tx(messages::findAll);
    }

    public RatePlans ratePlans() {
        return plans;
    }

    public Reservation create(CreateReservation cmd) {
        return db.tx(c -> {
            Room room = rooms.findForUpdate(c, cmd.roomNumber())
                    .orElseThrow(() -> ApiException.roomNotFound(cmd.roomNumber()));
            if (cmd.guests() > room.capacity()) {
                throw ApiException.capacityExceeded(room.capacity());
            }
            RatePlan plan = plans.get(cmd.ratePlan());
            PriceQuote quote = plan.quote(new PricingRequest(room, cmd.checkIn(), cmd.checkOut(), cmd.companyCode()));
            if (reservations.existsOverlap(c, room.number(), cmd.checkIn(), cmd.checkOut())) {
                throw ApiException.roomUnavailable();
            }
            String company = "CORPORATE".equals(plan.code()) ? cmd.companyCode() : null;
            return reservations.insert(c, new Reservation(0, room.number(), cmd.guestName(), cmd.guestEmail(),
                    cmd.guestCpf(), cmd.checkIn(), cmd.checkOut(), cmd.guests(), plan.code(), quote.nights(),
                    quote.subtotal(), quote.discount(), quote.total(), ReservationStatus.PENDING,
                    Money.ZERO, null, company));
        });
    }

    public Reservation get(String id) {
        return db.tx(c -> load(c, id, false));
    }

    public Reservation confirm(String id) {
        return db.tx(c -> {
            Reservation confirmed = transition(c, id, Reservation::confirm);
            Room room = rooms.find(c, confirmed.roomNumber()).orElseThrow();
            String receipt = ReceiptFormatter.format(Document.of(confirmed, room));
            messages.save(c, new Message(0, "EMAIL", confirmed.guestEmail(),
                    "Reserva confirmada - Pousada Maré Alta", confirmationBody(confirmed, receipt)));
            return confirmed;
        });
    }

    public Reservation cancel(String id) {
        return db.tx(c -> {
            Reservation current = load(c, id, true);
            Reservation cancelled = current.cancel(plans.get(current.ratePlan()).refund(current.amountPaid()));
            reservations.update(c, cancelled);
            return cancelled;
        });
    }

    public Reservation checkIn(String id) {
        return db.tx(c -> transition(c, id, Reservation::checkInGuest));
    }

    public Document checkOut(String id) {
        return db.tx(c -> documentOf(c, transition(c, id, Reservation::checkOutGuest)));
    }

    public Document document(String id) {
        return db.tx(c -> {
            Reservation r = load(c, id, false);
            if (!r.status().isPaid()) {
                throw ApiException.operationNotAllowed("Só existe segunda via de reserva paga");
            }
            return documentOf(c, r);
        });
    }


    private Document documentOf(Connection c, Reservation r) throws SQLException {
        return Document.of(r, rooms.find(c, r.roomNumber()).orElseThrow());
    }

    private Reservation transition(Connection c, String id, UnaryOperator<Reservation> step) throws SQLException {
        Reservation next = step.apply(load(c, id, true));
        reservations.update(c, next);
        return next;
    }

    private Reservation load(Connection c, String id, boolean lock) throws SQLException {
        long parsed;
        try {
            parsed = Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw ApiException.reservationNotFound(id);
        }
        return reservations.find(c, parsed, lock).orElseThrow(() -> ApiException.reservationNotFound(id));
    }

    private String confirmationBody(Reservation r, String receipt) {
        return "Olá, " + r.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + receipt + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
    }
}
