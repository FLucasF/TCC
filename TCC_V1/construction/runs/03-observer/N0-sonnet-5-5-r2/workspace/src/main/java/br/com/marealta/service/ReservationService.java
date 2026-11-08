package br.com.marealta.service;

import br.com.marealta.domain.ApiException;
import br.com.marealta.domain.ErrorCode;
import br.com.marealta.domain.Message;
import br.com.marealta.domain.PricingCalculator;
import br.com.marealta.domain.Quote;
import br.com.marealta.domain.Receipt;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.domain.Money;
import br.com.marealta.domain.Room;
import br.com.marealta.events.ReservationEvent;
import br.com.marealta.events.ReservationEvents;

import java.util.List;

public class ReservationService {
    private final Transactions tx;
    private final RoomRepository rooms;
    private final ReservationRepository reservations;
    private final MessageStore messages;
    private final ReservationEvents events;

    public ReservationService(Transactions tx, RoomRepository rooms, ReservationRepository reservations,
                              MessageStore messages, ReservationEvents events) {
        this.tx = tx;
        this.rooms = rooms;
        this.reservations = reservations;
        this.messages = messages;
        this.events = events;
    }

    public List<Room> listRooms() {
        return tx.run(rooms::findAll);
    }

    public List<Message> listMessages() {
        return tx.run(messages::findAll);
    }

    public Reservation create(NewReservation cmd) {
        if (!cmd.checkOut().isAfter(cmd.checkIn())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "A saída deve ser depois da entrada.");
        }
        if (cmd.guests() < 1) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "É preciso pelo menos um hóspede.");
        }
        return tx.run(() -> {
            Room room = rooms.findForUpdate(cmd.roomNumber())
                    .orElseThrow(() -> new ApiException(ErrorCode.ROOM_NOT_FOUND, "Quarto inexistente."));
            if (cmd.guests() > room.capacity()) {
                throw new ApiException(ErrorCode.CAPACITY_EXCEEDED,
                        "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes.");
            }
            if (reservations.existsOverlapping(room.number(), cmd.checkIn(), cmd.checkOut())) {
                throw new ApiException(ErrorCode.ROOM_UNAVAILABLE, "Quarto já reservado em alguma das noites.");
            }
            Quote quote = PricingCalculator.quote(room, cmd.checkIn(), cmd.checkOut(), cmd.ratePlan());
            Reservation draft = new Reservation(0, room.number(), cmd.guestName(), cmd.guestEmail(),
                    cmd.guestCpf(), cmd.checkIn(), cmd.checkOut(), cmd.guests(), cmd.ratePlan(), quote.nights(),
                    quote.subtotal(), quote.discount(), quote.total(), ReservationStatus.PENDING, Money.ZERO, null);
            return reservations.insert(draft);
        });
    }

    public Reservation get(long id) {
        return tx.run(() -> reservations.find(id).orElseThrow(ReservationService::notFound));
    }

    public Reservation confirm(long id) {
        return tx.run(() -> {
            Reservation current = lock(id);
            Reservation updated = current.confirm();
            reservations.update(updated);
            events.publish(new ReservationEvent.Confirmed(updated, roomOf(updated)));
            return updated;
        });
    }

    public Reservation cancel(long id) {
        return tx.run(() -> {
            Reservation current = lock(id);
            Reservation updated = current.cancel();
            reservations.update(updated);
            events.publish(new ReservationEvent.Cancelled(updated, roomOf(updated), current.status()));
            return updated;
        });
    }

    public Reservation checkIn(long id) {
        return tx.run(() -> {
            Reservation updated = lock(id).checkInGuest();
            reservations.update(updated);
            return updated;
        });
    }

    public Receipt checkOut(long id) {
        return tx.run(() -> {
            Reservation updated = lock(id).checkOutGuest();
            reservations.update(updated);
            return Receipt.of(updated, roomOf(updated));
        });
    }

    /** Segunda via do comprovante: só existe para reserva paga. */
    public Receipt document(long id) {
        return tx.run(() -> {
            Reservation r = reservations.find(id).orElseThrow(ReservationService::notFound);
            if (!r.status().isPaid()) {
                throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                        "O comprovante só existe para reservas pagas.");
            }
            return Receipt.of(r, roomOf(r));
        });
    }

    private Reservation lock(long id) {
        return reservations.findForUpdate(id).orElseThrow(ReservationService::notFound);
    }

    private Room roomOf(Reservation r) {
        return rooms.find(r.roomNumber()).orElseThrow(() -> new IllegalStateException("Quarto sumiu: " + r.roomNumber()));
    }

    private static ApiException notFound() {
        return new ApiException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva inexistente.");
    }
}
