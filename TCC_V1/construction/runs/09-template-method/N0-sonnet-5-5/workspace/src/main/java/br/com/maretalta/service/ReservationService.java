package br.com.maretalta.service;

import br.com.maretalta.domain.Message;
import br.com.maretalta.domain.Money;
import br.com.maretalta.domain.Pricing;
import br.com.maretalta.domain.RatePlan;
import br.com.maretalta.domain.Receipt;
import br.com.maretalta.domain.Reservation;
import br.com.maretalta.domain.Room;
import br.com.maretalta.domain.Status;
import br.com.maretalta.store.Store;

import java.time.LocalDate;
import java.util.List;

public class ReservationService {

    public record NewReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                 LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {}

    private final Store store;

    public ReservationService(Store store) {
        this.store = store;
    }

    public Reservation create(NewReservation n) {
        if (!n.checkOut().isAfter(n.checkIn())) throw ApiException.invalid("A saída deve ser depois da entrada");
        if (n.guests() < 1) throw ApiException.invalid("É preciso ao menos 1 hóspede");
        Room room = Room.find(n.roomNumber()).orElseThrow(() -> ApiException.roomNotFound(n.roomNumber()));
        if (n.guests() > room.capacity()) throw ApiException.capacityExceeded(room.capacity());

        Pricing.Quote q = Pricing.quote(room.type(), n.checkIn(), n.checkOut(), n.ratePlan());
        return store.inTransaction(tx -> {
            tx.lockRoom(room.number());
            if (tx.hasOverlap(room.number(), n.checkIn(), n.checkOut())) throw ApiException.roomUnavailable();
            return tx.insert(new Reservation(0, room.number(), n.guestName(), n.guestEmail(), n.guestCpf(),
                    n.checkIn(), n.checkOut(), n.guests(), n.ratePlan(), q.nights(), q.subtotal(), q.discount(),
                    q.total(), Status.PENDING, Money.ZERO, null));
        });
    }

    public Reservation get(String id) {
        return store.inTransaction(tx -> tx.find(parseId(id)).orElseThrow(() -> ApiException.reservationNotFound(id)));
    }

    /** Confirma (pagamento do total) e guarda o e-mail de confirmação na mesma transação. */
    public Reservation confirm(String id) {
        return store.inTransaction(tx -> {
            Reservation r = lock(tx, id);
            require(r, "confirmar", Status.PENDING);
            Reservation confirmed = r.withStatus(Status.CONFIRMED).withPayment(r.total());
            tx.update(confirmed);
            tx.addMessage(Mailer.CHANNEL, confirmed.guestEmail(), Mailer.CONFIRMATION_SUBJECT,
                    Mailer.confirmationBody(confirmed));
            return confirmed;
        });
    }

    public Reservation cancel(String id) {
        return store.inTransaction(tx -> {
            Reservation r = lock(tx, id);
            require(r, "cancelar", Status.PENDING, Status.CONFIRMED);
            boolean refunds = r.status() == Status.CONFIRMED && r.ratePlan().refundable();
            Reservation cancelled = r.withStatus(Status.CANCELLED)
                    .withRefund(refunds ? r.amountPaid() : Money.ZERO);
            tx.update(cancelled);
            return cancelled;
        });
    }

    public Reservation checkIn(String id) {
        return store.inTransaction(tx -> {
            Reservation r = lock(tx, id);
            require(r, "fazer check-in de", Status.CONFIRMED);
            Reservation updated = r.withStatus(Status.CHECKED_IN);
            tx.update(updated);
            return updated;
        });
    }

    public Receipt checkOut(String id) {
        return store.inTransaction(tx -> {
            Reservation r = lock(tx, id);
            require(r, "fazer check-out de", Status.CHECKED_IN);
            Reservation updated = r.withStatus(Status.CHECKED_OUT);
            tx.update(updated);
            return Receipt.of(updated);
        });
    }

    /** Segunda via: só para reserva paga. */
    public Receipt document(String id) {
        Reservation r = get(id);
        if (!r.status().isPaid()) {
            throw ApiException.notAllowed("Só há comprovante para reserva paga (situação atual: " + r.status() + ")");
        }
        return Receipt.of(r);
    }

    public List<Message> messages() {
        return store.inTransaction(Store.Tx::messages);
    }

    private static Reservation lock(Store.Tx tx, String id) {
        return tx.findForUpdate(parseId(id)).orElseThrow(() -> ApiException.reservationNotFound(id));
    }

    private static void require(Reservation r, String action, Status... allowed) {
        for (Status s : allowed) if (r.status() == s) return;
        throw ApiException.notAllowed("Não é possível " + action + " uma reserva " + r.status());
    }

    private static long parseId(String id) {
        try {
            long v = Long.parseLong(id);
            if (v >= 1) return v;
        } catch (NumberFormatException ignored) {
            // cai no erro abaixo
        }
        throw ApiException.reservationNotFound(id);
    }
}
