package br.com.pousadamarealta.reservas.service;

import br.com.pousadamarealta.reservas.domain.PousadaException;
import br.com.pousadamarealta.reservas.domain.Pricing;
import br.com.pousadamarealta.reservas.domain.Money;
import br.com.pousadamarealta.reservas.domain.Quote;
import br.com.pousadamarealta.reservas.domain.Receipt;
import br.com.pousadamarealta.reservas.domain.Reservation;
import br.com.pousadamarealta.reservas.domain.ReservationStatus;
import br.com.pousadamarealta.reservas.domain.Room;
import br.com.pousadamarealta.reservas.events.EventBus;
import br.com.pousadamarealta.reservas.events.ReservationCancelled;
import br.com.pousadamarealta.reservas.events.ReservationConfirmed;
import br.com.pousadamarealta.reservas.persistence.Database;
import br.com.pousadamarealta.reservas.persistence.ReservationRepository;
import br.com.pousadamarealta.reservas.persistence.RoomRepository;

import java.util.List;

import static br.com.pousadamarealta.reservas.domain.PousadaException.Code.CAPACITY_EXCEEDED;
import static br.com.pousadamarealta.reservas.domain.PousadaException.Code.RESERVATION_NOT_FOUND;
import static br.com.pousadamarealta.reservas.domain.PousadaException.Code.ROOM_NOT_FOUND;
import static br.com.pousadamarealta.reservas.domain.PousadaException.Code.ROOM_UNAVAILABLE;

public class ReservationService {
    private final Database db;
    private final RoomRepository rooms;
    private final ReservationRepository reservations;
    private final EventBus events;

    public ReservationService(Database db, RoomRepository rooms, ReservationRepository reservations, EventBus events) {
        this.db = db;
        this.rooms = rooms;
        this.reservations = reservations;
        this.events = events;
    }

    public List<Room> listRooms() {
        return db.transaction(rooms::findAll);
    }

    public Reservation create(CreateReservation cmd) {
        return db.transaction(() -> {
            // o lock do quarto serializa reservas concorrentes dele, para a checagem de disponibilidade valer
            Room room = rooms.find(cmd.roomNumber(), true)
                    .orElseThrow(() -> new PousadaException(ROOM_NOT_FOUND, "Quarto " + cmd.roomNumber() + " não existe"));
            if (cmd.guests() > room.capacity()) {
                throw new PousadaException(CAPACITY_EXCEEDED,
                        "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes");
            }
            if (reservations.existsOverlap(room.number(), cmd.checkIn(), cmd.checkOut())) {
                throw new PousadaException(ROOM_UNAVAILABLE,
                        "O quarto " + room.number() + " já está reservado em alguma das noites pedidas");
            }
            Quote quote = Pricing.quote(room, cmd.checkIn(), cmd.checkOut(), cmd.ratePlan());
            return reservations.insert(new Reservation(0, room.number(), cmd.guestName(), cmd.guestEmail(),
                    cmd.guestCpf(), cmd.checkIn(), cmd.checkOut(), cmd.guests(), cmd.ratePlan(), quote.nights(),
                    quote.subtotal(), quote.discount(), quote.total(), ReservationStatus.PENDING, Money.ZERO, null));
        });
    }

    public Reservation get(long id) {
        return db.transaction(() -> load(id, false));
    }

    public Reservation confirm(long id) {
        return db.transaction(() -> {
            Reservation confirmed = load(id, true).confirm();
            reservations.update(confirmed);
            events.publish(new ReservationConfirmed(confirmed, roomOf(confirmed)));
            return confirmed;
        });
    }

    public Reservation cancel(long id) {
        return db.transaction(() -> {
            Reservation current = load(id, true);
            Reservation cancelled = current.cancel();
            reservations.update(cancelled);
            if (current.status() == ReservationStatus.CONFIRMED) {
                events.publish(new ReservationCancelled(cancelled, roomOf(cancelled)));
            }
            return cancelled;
        });
    }

    public Reservation checkIn(long id) {
        return db.transaction(() -> {
            Reservation updated = load(id, true).checkInGuest();
            reservations.update(updated);
            return updated;
        });
    }

    public Receipt checkOut(long id) {
        return db.transaction(() -> {
            Reservation updated = load(id, true).checkOutGuest();
            reservations.update(updated);
            return Receipt.of(updated, roomOf(updated));
        });
    }

    public Receipt document(long id) {
        return db.transaction(() -> {
            Reservation reservation = load(id, false);
            reservation.requirePaid();
            return Receipt.of(reservation, roomOf(reservation));
        });
    }

    private Reservation load(long id, boolean forUpdate) {
        return reservations.find(id, forUpdate)
                .orElseThrow(() -> new PousadaException(RESERVATION_NOT_FOUND, "Reserva " + id + " não existe"));
    }

    private Room roomOf(Reservation reservation) {
        return rooms.find(reservation.roomNumber(), false).orElseThrow();
    }
}
