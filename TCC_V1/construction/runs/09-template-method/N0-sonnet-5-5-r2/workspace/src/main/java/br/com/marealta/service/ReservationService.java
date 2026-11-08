package br.com.marealta.service;

import br.com.marealta.domain.Message;
import br.com.marealta.domain.Money;
import br.com.marealta.domain.Pricing;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.domain.Room;
import br.com.marealta.store.Store;
import br.com.marealta.store.Tx;
import java.time.LocalDate;
import java.util.List;

public class ReservationService {
    public static final String HOTEL_NAME = "Pousada Maré Alta";
    private static final String CHANNEL_EMAIL = "EMAIL";

    public record NewReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                 LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {}

    private final Store store;

    public ReservationService(Store store) {
        this.store = store;
    }

    public List<Room> rooms() {
        return store.transaction(Tx::rooms);
    }

    public List<Message> messages() {
        return store.transaction(Tx::messages);
    }

    public Reservation create(NewReservation req) {
        if (!req.checkOut().isAfter(req.checkIn())) {
            throw ApiException.invalid("A saída deve ser depois da entrada");
        }
        if (req.guests() < 1) {
            throw ApiException.invalid("É preciso ao menos um hóspede");
        }
        return store.transaction(tx -> {
            Room room = tx.room(req.roomNumber()).orElseThrow(() -> ApiException.roomNotFound(req.roomNumber()));
            if (req.guests() > room.capacity()) {
                throw new ApiException(422, "CAPACITY_EXCEEDED",
                        "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes");
            }
            tx.lockRoom(room.number());
            if (tx.hasActiveOverlap(room.number(), req.checkIn(), req.checkOut())) {
                throw new ApiException(409, "ROOM_UNAVAILABLE", "Quarto já reservado em alguma das noites");
            }
            Pricing.Quote quote = Pricing.quote(room, req.checkIn(), req.checkOut(), req.ratePlan());
            return tx.insert(new Reservation(0, room.number(), req.guestName(), req.guestEmail(), req.guestCpf(),
                    req.checkIn(), req.checkOut(), req.guests(), req.ratePlan(), quote.nights(), quote.subtotal(),
                    quote.discount(), quote.total(), ReservationStatus.PENDING, Money.ZERO, null));
        });
    }

    public Reservation get(String rawId) {
        return store.transaction(tx -> tx.reservation(parseId(rawId))
                .orElseThrow(() -> ApiException.reservationNotFound(rawId)));
    }

    public Reservation confirm(String rawId) {
        return store.transaction(tx -> {
            Reservation r = lock(tx, rawId);
            require(r, ReservationStatus.PENDING, "Só é possível confirmar uma reserva pendente");
            Reservation confirmed = r.withPayment(ReservationStatus.CONFIRMED, r.total());
            tx.update(confirmed);
            tx.addMessage(CHANNEL_EMAIL, confirmed.guestEmail(), "Reserva confirmada - " + HOTEL_NAME,
                    confirmationEmail(confirmed, receipt(tx, confirmed)));
            return confirmed;
        });
    }

    public Reservation cancel(String rawId) {
        return store.transaction(tx -> {
            Reservation r = lock(tx, rawId);
            if (r.status() != ReservationStatus.PENDING && r.status() != ReservationStatus.CONFIRMED) {
                throw ApiException.notAllowed("Só é possível cancelar antes do check-in");
            }
            boolean refundable = r.status() == ReservationStatus.CONFIRMED && r.ratePlan() == RatePlan.STANDARD;
            Reservation cancelled = r.withCancellation(refundable ? r.amountPaid() : Money.ZERO);
            tx.update(cancelled);
            return cancelled;
        });
    }

    public Reservation checkIn(String rawId) {
        return store.transaction(tx -> {
            Reservation r = lock(tx, rawId);
            require(r, ReservationStatus.CONFIRMED, "Só é possível fazer check-in de uma reserva confirmada");
            Reservation next = r.withStatus(ReservationStatus.CHECKED_IN);
            tx.update(next);
            return next;
        });
    }

    public Receipt checkOut(String rawId) {
        return store.transaction(tx -> {
            Reservation r = lock(tx, rawId);
            require(r, ReservationStatus.CHECKED_IN, "Só é possível fazer check-out após o check-in");
            Reservation next = r.withStatus(ReservationStatus.CHECKED_OUT);
            tx.update(next);
            return receipt(tx, next);
        });
    }

    /** Segunda via: só para reserva paga. */
    public Receipt document(String rawId) {
        return store.transaction(tx -> {
            Reservation r = tx.reservation(parseId(rawId))
                    .orElseThrow(() -> ApiException.reservationNotFound(rawId));
            if (!r.status().isPaid()) {
                throw ApiException.notAllowed("O comprovante só existe para reserva paga");
            }
            return receipt(tx, r);
        });
    }

    private static Receipt receipt(Tx tx, Reservation r) {
        Room room = tx.room(r.roomNumber()).orElseThrow();
        return Receipt.of(r, room.type());
    }

    static String confirmationEmail(Reservation r, Receipt receipt) {
        return "Olá, " + r.guestName() + "!\n\n"
                + "Sua reserva na " + HOTEL_NAME + " está confirmada. Estes são os dados dela:\n\n"
                + receipt.toText() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
    }

    private static Reservation lock(Tx tx, String rawId) {
        return tx.reservationForUpdate(parseId(rawId)).orElseThrow(() -> ApiException.reservationNotFound(rawId));
    }

    private static void require(Reservation r, ReservationStatus expected, String message) {
        if (r.status() != expected) {
            throw ApiException.notAllowed(message);
        }
    }

    /** Id que não é número nunca existe. */
    private static long parseId(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw ApiException.reservationNotFound(raw);
        }
    }
}
