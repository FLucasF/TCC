package pousada.service;

import pousada.domain.*;
import pousada.persistence.Database;
import pousada.persistence.MessageRepository;
import pousada.persistence.ReservationRepository;
import pousada.persistence.RoomRepository;

import java.sql.Connection;
import java.util.List;
import java.util.function.UnaryOperator;

public class ReservationService {
    private final Database database;
    private final RoomRepository rooms;
    private final ReservationRepository reservations;
    private final MessageRepository messages;
    private final RatePlanCatalog plans;

    public ReservationService(Database database, RoomRepository rooms, ReservationRepository reservations,
                              MessageRepository messages, RatePlanCatalog plans) {
        this.database = database;
        this.rooms = rooms;
        this.reservations = reservations;
        this.messages = messages;
        this.plans = plans;
    }

    public List<Room> rooms() {
        return database.inTransaction(rooms::findAll);
    }

    public List<Message> messages() {
        return database.inTransaction(messages::findAll);
    }

    public Reservation get(String id) {
        return database.inTransaction(c -> reservations.find(c, parseId(id))
                .orElseThrow(() -> ApiException.reservationNotFound(id)));
    }

    public Receipt document(String id) {
        return database.inTransaction(c -> {
            Reservation reservation = reservations.find(c, parseId(id))
                    .orElseThrow(() -> ApiException.reservationNotFound(id)).requirePaid();
            return receiptOf(c, reservation);
        });
    }

    public Reservation create(CreateReservation request) {
        if (!request.checkOut().isAfter(request.checkIn())) {
            throw ApiException.invalidRequest("A saída deve ser depois da entrada");
        }
        if (request.guests() < 1) {
            throw ApiException.invalidRequest("É preciso ao menos um hóspede");
        }
        RatePlan plan = plans.find(request.ratePlan())
                .orElseThrow(() -> ApiException.invalidRequest("Plano de tarifa desconhecido: " + request.ratePlan()));
        return database.inTransaction(c -> {
            Room room = rooms.findForUpdate(c, request.roomNumber())
                    .orElseThrow(() -> ApiException.roomNotFound(request.roomNumber()));
            if (request.guests() > room.capacity()) {
                throw ApiException.capacityExceeded(room.capacity());
            }
            Booking booking = new Booking(room, request.checkIn(), request.checkOut(), request.guests(),
                    request.companyCode());
            plan.validate(booking);
            if (reservations.hasOverlap(c, room.number(), request.checkIn(), request.checkOut())) {
                throw ApiException.roomUnavailable();
            }
            Quote quote = plan.quote(booking);
            return reservations.insert(c, new Reservation(0, room.number(), request.guestName(),
                    request.guestEmail(), request.guestCpf(), request.checkIn(), request.checkOut(),
                    request.guests(), plan.code(), booking.nights(), quote.subtotal(), quote.discount(),
                    quote.total(), ReservationStatus.PENDING, Money.ZERO, null));
        });
    }

    public Reservation confirm(String id) {
        return database.inTransaction(c -> {
            Reservation confirmed = transition(c, id, Reservation::confirm);
            messages.insert(c, Message.confirmation(confirmed, receiptOf(c, confirmed)));
            return confirmed;
        });
    }

    public Reservation cancel(String id) {
        return database.inTransaction(c -> transition(c, id, r -> r.cancel(planOf(r))));
    }

    public Reservation checkIn(String id) {
        return database.inTransaction(c -> transition(c, id, Reservation::checkInGuest));
    }

    public Receipt checkOut(String id) {
        return database.inTransaction(c -> receiptOf(c, transition(c, id, Reservation::checkOutGuest)));
    }

    private Reservation transition(Connection c, String id, UnaryOperator<Reservation> change) {
        Reservation current = reservations.findForUpdate(c, parseId(id))
                .orElseThrow(() -> ApiException.reservationNotFound(id));
        Reservation updated = change.apply(current);
        reservations.updateState(c, updated);
        return updated;
    }

    private RatePlan planOf(Reservation reservation) {
        return plans.find(reservation.ratePlan()).orElseThrow();
    }

    private Receipt receiptOf(Connection c, Reservation reservation) {
        return Receipt.of(reservation, rooms.find(c, reservation.roomNumber()).orElseThrow());
    }

    private long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw ApiException.reservationNotFound(id);
        }
    }
}
