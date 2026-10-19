package br.com.marealta.service;

import br.com.marealta.domain.DomainException;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;
import br.com.marealta.messaging.Channel;
import br.com.marealta.messaging.Notifier;

public class ReservationService {
    static final String CONFIRMATION_SUBJECT = "Reserva confirmada - Pousada Maré Alta";

    private final RoomCatalog rooms;
    private final ReservationRepository repository;
    private final Notifier notifier;

    public ReservationService(RoomCatalog rooms, ReservationRepository repository, Notifier notifier) {
        this.rooms = rooms;
        this.repository = repository;
        this.notifier = notifier;
    }

    public Reservation create(NewReservation request) {
        if (!request.checkOut().isAfter(request.checkIn())) {
            throw DomainException.invalidRequest("A saída deve ser depois da entrada");
        }
        if (request.guests() < 1) {
            throw DomainException.invalidRequest("É preciso pelo menos um hóspede");
        }
        Room room = rooms.find(request.roomNumber())
                .orElseThrow(() -> DomainException.roomNotFound(String.valueOf(request.roomNumber())));
        if (request.guests() > room.capacity()) {
            throw DomainException.capacityExceeded(room.capacity());
        }
        synchronized (repository) {
            boolean taken = repository.all().stream()
                    .anyMatch(r -> r.room().equals(room) && r.overlaps(request.checkIn(), request.checkOut()));
            if (taken) throw DomainException.roomUnavailable();
            Reservation reservation = new Reservation(repository.nextId(), room, request.guestName(),
                    request.guestEmail(), request.guestCpf(), request.checkIn(), request.checkOut(),
                    request.guests(), request.ratePlan());
            repository.add(reservation);
            return reservation;
        }
    }

    public Reservation get(String id) {
        return repository.find(parseId(id)).orElseThrow(() -> DomainException.reservationNotFound(id));
    }

    public Reservation confirm(String id) {
        synchronized (repository) {
            Reservation reservation = get(id);
            reservation.confirm();
            notifier.send(Channel.EMAIL, reservation.guestEmail(), CONFIRMATION_SUBJECT,
                    ReceiptFormatter.confirmationEmail(reservation.guestName(), Receipt.of(reservation)));
            return reservation;
        }
    }

    public Reservation cancel(String id) {
        synchronized (repository) {
            Reservation reservation = get(id);
            reservation.cancel();
            return reservation;
        }
    }

    public Reservation checkIn(String id) {
        synchronized (repository) {
            Reservation reservation = get(id);
            reservation.performCheckIn();
            return reservation;
        }
    }

    public Receipt checkOut(String id) {
        synchronized (repository) {
            Reservation reservation = get(id);
            reservation.performCheckOut();
            return Receipt.of(reservation);
        }
    }

    /** Segunda via: só existe para reserva paga. */
    public Receipt document(String id) {
        synchronized (repository) {
            Reservation reservation = get(id);
            if (!reservation.status().isPaid()) {
                throw DomainException.notAllowed("A segunda via só existe para reserva paga");
            }
            return Receipt.of(reservation);
        }
    }

    private static int parseId(String id) {
        try {
            return Integer.parseInt(id);
        } catch (NumberFormatException e) {
            throw DomainException.reservationNotFound(id);
        }
    }
}
