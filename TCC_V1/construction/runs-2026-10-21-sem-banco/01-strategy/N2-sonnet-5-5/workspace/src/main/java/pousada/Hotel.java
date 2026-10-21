package pousada;

import pousada.plan.RatePlan;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Rooms, reservations and the operations the reception and the site perform on them. */
public class Hotel {
    private final List<Room> rooms = List.of(
            new Room(101, RoomType.STANDARD), new Room(102, RoomType.STANDARD),
            new Room(201, RoomType.DELUXE), new Room(202, RoomType.DELUXE),
            new Room(301, RoomType.SUITE));
    private final List<Reservation> reservations = new ArrayList<>();
    private final Outbox outbox;

    public Hotel(Outbox outbox) {
        this.outbox = outbox;
    }

    public List<Room> rooms() { return rooms; }

    public synchronized List<Reservation> reservations() { return List.copyOf(reservations); }

    public Outbox outbox() { return outbox; }

    public synchronized Reservation create(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                           LocalDate checkIn, LocalDate checkOut, int guests, RatePlan plan,
                                           String companyCode) {
        Room room = rooms.stream().filter(r -> r.number() == roomNumber).findFirst()
                .orElseThrow(() -> new PousadaException(ErrorCode.ROOM_NOT_FOUND, "Quarto " + roomNumber + " não existe"));
        if (guests > room.capacity()) {
            throw new PousadaException(ErrorCode.CAPACITY_EXCEEDED,
                    "O quarto " + roomNumber + " comporta no máximo " + room.capacity() + " hóspedes");
        }
        Stay stay = new Stay(room, checkIn, checkOut, companyCode);
        plan.validate(stay);
        boolean taken = reservations.stream().anyMatch(r -> r.room.equals(room)
                && r.status() != ReservationStatus.CANCELLED && r.overlaps(checkIn, checkOut));
        if (taken) {
            throw new PousadaException(ErrorCode.ROOM_UNAVAILABLE, "Quarto já reservado em alguma das noites");
        }
        Reservation reservation = new Reservation(reservations.size() + 1, room, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, plan, plan.quote(stay));
        reservations.add(reservation);
        return reservation;
    }

    public synchronized Reservation find(String id) {
        try {
            int index = Integer.parseInt(id);
            if (index >= 1 && index <= reservations.size()) return reservations.get(index - 1);
        } catch (NumberFormatException ignored) {
            // falls through to not found
        }
        throw new PousadaException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva " + id + " não existe");
    }

    public synchronized Reservation confirm(String id) {
        Reservation r = find(id);
        r.confirm();
        outbox.sendEmail(r.guestEmail, "Reserva confirmada - Pousada Maré Alta",
                "Olá, " + r.guestName + "!\n\n"
                        + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                        + Receipt.of(r).text() + "\n\n"
                        + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                        + "Até breve!");
        return r;
    }

    public synchronized Reservation cancel(String id) {
        Reservation r = find(id);
        r.cancel();
        return r;
    }

    public synchronized Reservation checkIn(String id) {
        Reservation r = find(id);
        r.checkIn();
        return r;
    }

    public synchronized Receipt checkOut(String id) {
        Reservation r = find(id);
        r.checkOut();
        return Receipt.of(r);
    }

    public synchronized Receipt document(String id) {
        Reservation r = find(id);
        if (!r.status().isPaid()) {
            throw new PousadaException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "A segunda via só existe para reserva paga");
        }
        return Receipt.of(r);
    }
}
