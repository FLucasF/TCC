package br.com.marealta.reservas.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationService {
    public static final String INN_NAME = "Pousada Maré Alta";
    public static final String CHANNEL_EMAIL = "EMAIL";

    public record NewReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                 LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
    }

    private final RoomCatalog rooms;
    private final MessageSender messages;
    private final List<Reservation> reservations = new ArrayList<>();

    public ReservationService(RoomCatalog rooms, MessageSender messages) {
        this.rooms = rooms;
        this.messages = messages;
    }

    public synchronized Reservation create(NewReservation req) {
        if (!req.checkOut().isAfter(req.checkIn())) {
            throw new BookingException(ErrorCode.INVALID_REQUEST, "A saída deve ser depois da entrada");
        }
        if (req.guests() < 1) {
            throw new BookingException(ErrorCode.INVALID_REQUEST, "É preciso ao menos um hóspede");
        }
        Room room = rooms.find(req.roomNumber()).orElseThrow(() ->
                new BookingException(ErrorCode.ROOM_NOT_FOUND, "Quarto " + req.roomNumber() + " não existe"));
        if (req.guests() > room.capacity()) {
            throw new BookingException(ErrorCode.CAPACITY_EXCEEDED,
                    "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes");
        }
        boolean taken = reservations.stream()
                .filter(r -> r.room().equals(room) && r.status() != ReservationStatus.CANCELLED)
                .anyMatch(r -> r.overlaps(req.checkIn(), req.checkOut()));
        if (taken) {
            throw new BookingException(ErrorCode.ROOM_UNAVAILABLE,
                    "O quarto " + room.number() + " já está reservado em alguma das noites");
        }
        Reservation reservation = new Reservation(reservations.size() + 1, room, req.guestName(),
                req.guestEmail(), req.guestCpf(), req.checkIn(), req.checkOut(), req.guests(),
                req.ratePlan(), Pricing.quote(room, req.checkIn(), req.checkOut(), req.ratePlan()));
        reservations.add(reservation);
        return reservation;
    }

    public synchronized Reservation get(int id) {
        if (id < 1 || id > reservations.size()) {
            throw new BookingException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva " + id + " não existe");
        }
        return reservations.get(id - 1);
    }

    public synchronized Reservation confirm(int id) {
        Reservation r = get(id);
        r.confirm();
        String receipt = Receipt.of(r).text();
        messages.send(CHANNEL_EMAIL, r.guestEmail(), "Reserva confirmada - " + INN_NAME,
                "Olá, " + r.guestName() + "!\n\n"
                        + "Sua reserva na " + INN_NAME + " está confirmada. Estes são os dados dela:\n\n"
                        + receipt + "\n\n"
                        + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                        + "Até breve!");
        return r;
    }

    public synchronized Reservation cancel(int id) {
        Reservation r = get(id);
        r.cancel();
        return r;
    }

    public synchronized Reservation checkIn(int id) {
        Reservation r = get(id);
        r.checkInGuest();
        return r;
    }

    public synchronized Receipt checkOut(int id) {
        Reservation r = get(id);
        r.checkOutGuest();
        return Receipt.of(r);
    }

    /** Segunda via: só existe para reserva paga. */
    public synchronized Receipt document(int id) {
        Reservation r = get(id);
        if (!r.status().isPaid()) {
            throw new BookingException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Só há comprovante para reserva paga");
        }
        return Receipt.of(r);
    }

    public synchronized List<Reservation> all() {
        return List.copyOf(reservations);
    }

    public RoomCatalog rooms() {
        return rooms;
    }
}
