package br.com.marealta;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class ReservationService {
    static final String CONFIRMATION_SUBJECT = "Reserva confirmada - Pousada Maré Alta";

    private final List<Reservation> reservations = new ArrayList<>();
    private final Outbox outbox;

    ReservationService(Outbox outbox) {
        this.outbox = outbox;
    }

    record NewReservation(int roomNumber, String guestName, String guestEmail, Identification identification,
                          LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {}

    synchronized Reservation create(NewReservation n) {
        Room room = Room.ALL.stream().filter(r -> r.number() == n.roomNumber()).findFirst()
                .orElseThrow(() -> new ApiException(404, "ROOM_NOT_FOUND", "Quarto " + n.roomNumber() + " não existe"));
        if (n.guests() > room.capacity()) {
            throw new ApiException(422, "CAPACITY_EXCEEDED",
                    "O quarto " + room.number() + " comporta até " + room.capacity() + " hóspedes");
        }
        boolean taken = reservations.stream().anyMatch(r -> r.room.equals(room)
                && r.status != Status.CANCELLED && r.overlaps(n.checkIn(), n.checkOut()));
        if (taken) {
            throw new ApiException(409, "ROOM_UNAVAILABLE", "Quarto já reservado em alguma das noites pedidas");
        }
        Pricing.Quote quote = Pricing.quote(room, n.checkIn(), n.checkOut(), n.ratePlan());
        Reservation r = new Reservation(reservations.size() + 1, room, n.guestName(), n.guestEmail(),
                n.identification(), n.checkIn(), n.checkOut(), n.guests(), n.ratePlan(), quote);
        reservations.add(r);
        return r;
    }

    synchronized Reservation get(String id) {
        try {
            int i = Integer.parseInt(id);
            if (i >= 1 && i <= reservations.size()) {
                return reservations.get(i - 1);
            }
        } catch (NumberFormatException ignored) {
            // falls through to not found
        }
        throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva " + id + " não existe");
    }

    synchronized Reservation confirm(String id) {
        Reservation r = get(id);
        require(r, "confirmar", Status.PENDING);
        r.status = Status.CONFIRMED;
        r.amountPaid = r.quote.total();
        String doc = Documents.render(r).text();
        String body = "Olá, " + r.guestName + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + doc + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        outbox.sendEmail(r.guestEmail, CONFIRMATION_SUBJECT, body);
        return r;
    }

    synchronized Reservation cancel(String id) {
        Reservation r = get(id);
        require(r, "cancelar", Status.PENDING, Status.CONFIRMED);
        r.refund = r.ratePlan == RatePlan.STANDARD ? r.amountPaid : Money.ZERO;
        r.status = Status.CANCELLED;
        return r;
    }

    synchronized Reservation checkIn(String id) {
        Reservation r = get(id);
        require(r, "fazer check-in de", Status.CONFIRMED);
        r.status = Status.CHECKED_IN;
        return r;
    }

    synchronized Document checkOut(String id) {
        Reservation r = get(id);
        require(r, "fazer check-out de", Status.CHECKED_IN);
        r.status = Status.CHECKED_OUT;
        return Documents.render(r);
    }

    synchronized Document duplicate(String id) {
        Reservation r = get(id);
        if (!r.status.isPaid()) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED",
                    "Segunda via só existe para reserva paga; situação atual: " + r.status);
        }
        return Documents.render(r);
    }

    synchronized List<Reservation> all() {
        return List.copyOf(reservations);
    }

    private static void require(Reservation r, String action, Status... allowed) {
        for (Status s : allowed) {
            if (r.status == s) {
                return;
            }
        }
        throw new ApiException(409, "OPERATION_NOT_ALLOWED",
                "Não é possível " + action + " reserva com situação " + r.status);
    }
}
