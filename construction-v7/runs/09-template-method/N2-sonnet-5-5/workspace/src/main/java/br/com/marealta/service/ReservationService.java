package br.com.marealta.service;

import br.com.marealta.domain.BusinessException;
import br.com.marealta.domain.ErrorCode;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.domain.Room;

import java.util.List;

public class ReservationService {
    private static final String EMAIL = "EMAIL";

    private final RoomCatalog rooms;
    private final ReservationRepository repository;
    private final MessageSender sender;

    public ReservationService(RoomCatalog rooms, ReservationRepository repository, MessageSender sender) {
        this.rooms = rooms;
        this.repository = repository;
        this.sender = sender;
    }

    public synchronized Reservation create(BookingRequest req) {
        if (req.guests() < 1 || !req.checkOut().isAfter(req.checkIn())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Datas ou número de hóspedes inválidos");
        }
        Room room = rooms.find(req.roomNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND, "Quarto inexistente"));
        if (req.guests() > room.capacity()) {
            throw new BusinessException(ErrorCode.CAPACITY_EXCEEDED, "O quarto comporta até " + room.capacity() + " pessoas");
        }
        boolean taken = repository.all().stream()
                .filter(r -> r.room().number() == room.number() && r.status() != ReservationStatus.CANCELLED)
                .anyMatch(r -> r.overlaps(req.checkIn(), req.checkOut()));
        if (taken) {
            throw new BusinessException(ErrorCode.ROOM_UNAVAILABLE, "Quarto já reservado em alguma das noites");
        }
        Reservation reservation = new Reservation(repository.nextId(), room, req.guestName(), req.guestEmail(),
                req.guestCpf(), req.checkIn(), req.checkOut(), req.guests(), req.ratePlan());
        repository.save(reservation);
        return reservation;
    }

    public synchronized Reservation get(int id) {
        return repository.find(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva inexistente"));
    }

    public synchronized Reservation confirm(int id) {
        Reservation r = get(id);
        r.confirm();
        sender.send(EMAIL, r.guestEmail(), "Reserva confirmada - Pousada Maré Alta", confirmationBody(r));
        return r;
    }

    public synchronized Reservation cancel(int id) {
        Reservation r = get(id);
        r.cancel();
        return r;
    }

    public synchronized Reservation checkIn(int id) {
        Reservation r = get(id);
        r.checkIn();
        return r;
    }

    public synchronized Receipt checkOut(int id) {
        Reservation r = get(id);
        r.checkOut();
        return Receipt.of(r);
    }

    /** Segunda via: só para reserva paga. */
    public synchronized Receipt receipt(int id) {
        Reservation r = get(id);
        if (!r.isPaid()) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "Comprovante só existe para reserva paga");
        }
        return Receipt.of(r);
    }

    public synchronized List<Reservation> all() {
        return repository.all();
    }

    private String confirmationBody(Reservation r) {
        return "Olá, " + r.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + ReceiptFormatter.format(r) + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
    }
}
