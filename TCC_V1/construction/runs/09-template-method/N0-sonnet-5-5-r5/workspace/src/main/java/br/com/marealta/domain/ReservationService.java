package br.com.marealta.domain;

import br.com.marealta.store.Store;
import br.com.marealta.store.Tx;
import java.math.BigDecimal;
import java.util.List;

public class ReservationService {
    private static final String CHANNEL_EMAIL = "EMAIL";
    private static final String CONFIRMATION_SUBJECT = "Reserva confirmada - Pousada Maré Alta";

    private final Store store;

    public ReservationService(Store store) {
        this.store = store;
    }

    public List<Room> rooms() {
        return store.tx(Tx::rooms);
    }

    public List<Message> messages() {
        return store.tx(Tx::messages);
    }

    public Reservation get(long id) {
        return store.tx(tx -> load(tx, id, false));
    }

    public Reservation create(NewReservation req) {
        if (!req.checkOut().isAfter(req.checkIn())) {
            throw DomainException.invalid("A saída deve ser depois da entrada");
        }
        if (req.guests() < 1) {
            throw DomainException.invalid("É preciso ao menos um hóspede");
        }
        return store.tx(tx -> {
            Room room = tx.room(req.roomNumber(), true)
                    .orElseThrow(() -> DomainException.roomNotFound(req.roomNumber()));
            if (req.guests() > room.capacity()) {
                throw DomainException.capacity("O quarto " + room.number() + " comporta até "
                        + room.capacity() + " pessoas");
            }
            if (tx.overlaps(room.number(), req.checkIn(), req.checkOut())) {
                throw DomainException.unavailable("O quarto " + room.number() + " já está reservado nesse período");
            }
            Quote quote = Pricing.quote(room, req.checkIn(), req.checkOut(), req.ratePlan());
            return tx.insert(new Reservation(0, room.number(), req.guestName(), req.guestEmail(), req.guestCpf(),
                    req.checkIn(), req.checkOut(), req.guests(), req.ratePlan(), quote.nights(), quote.subtotal(),
                    quote.discount(), quote.total(), Status.PENDING, Money.ZERO, null));
        });
    }

    public Reservation confirm(long id) {
        return store.tx(tx -> {
            Reservation r = load(tx, id, true);
            requireStatus(r, "confirmada", Status.PENDING);
            Reservation confirmed = r.confirmed();
            tx.update(confirmed);
            String text = ReceiptFormatter.text(receipt(tx, confirmed));
            tx.addMessage(CHANNEL_EMAIL, confirmed.guestEmail(), CONFIRMATION_SUBJECT,
                    "Olá, " + confirmed.guestName() + "!\n\n"
                            + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                            + text + "\n\n"
                            + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                            + "Até breve!");
            return confirmed;
        });
    }

    public Reservation cancel(long id) {
        return store.tx(tx -> {
            Reservation r = load(tx, id, true);
            requireStatus(r, "cancelada", Status.PENDING, Status.CONFIRMED);
            BigDecimal refund = r.ratePlan().refundable() ? r.amountPaid() : Money.ZERO;
            Reservation cancelled = r.cancelled(refund);
            tx.update(cancelled);
            return cancelled;
        });
    }

    public Reservation checkIn(long id) {
        return store.tx(tx -> {
            Reservation r = load(tx, id, true);
            requireStatus(r, "usada para check-in", Status.CONFIRMED);
            Reservation updated = r.withStatus(Status.CHECKED_IN);
            tx.update(updated);
            return updated;
        });
    }

    public Receipt checkOut(long id) {
        return store.tx(tx -> {
            Reservation r = load(tx, id, true);
            requireStatus(r, "usada para check-out", Status.CHECKED_IN);
            Reservation updated = r.withStatus(Status.CHECKED_OUT);
            tx.update(updated);
            return receipt(tx, updated);
        });
    }

    /** Segunda via: só existe para reserva paga. */
    public Receipt document(long id) {
        return store.tx(tx -> {
            Reservation r = load(tx, id, false);
            if (!r.status().isPaid()) {
                throw DomainException.notAllowed("A reserva " + id + " não tem comprovante (situação " + r.status() + ")");
            }
            return receipt(tx, r);
        });
    }

    private static Reservation load(Tx tx, long id, boolean lock) {
        return tx.reservation(id, lock).orElseThrow(() -> DomainException.reservationNotFound(id));
    }

    private static Receipt receipt(Tx tx, Reservation r) {
        Room room = tx.room(r.roomNumber(), false).orElseThrow(() -> DomainException.roomNotFound(r.roomNumber()));
        return Receipt.of(r, room);
    }

    private static void requireStatus(Reservation r, String action, Status... allowed) {
        for (Status s : allowed) {
            if (r.status() == s) {
                return;
            }
        }
        throw DomainException.notAllowed("A reserva " + r.id() + " está " + r.status() + " e não pode ser " + action);
    }
}
