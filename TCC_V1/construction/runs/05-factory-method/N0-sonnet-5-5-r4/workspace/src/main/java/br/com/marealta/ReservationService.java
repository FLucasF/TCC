package br.com.marealta;

import br.com.marealta.Documents.Document;
import br.com.marealta.Model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public final class ReservationService {
    private final Store store;

    public ReservationService(Store store) {
        this.store = store;
    }

    public record NewReservation(int roomNumber, String guestName, String guestEmail, Identification identification,
                                 LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {}

    public record Issued(Reservation reservation, Room room, Document document) {}

    public List<Room> rooms() {
        return store.tx(store::rooms);
    }

    public List<Message> messages() {
        return store.tx(store::messages);
    }

    public Reservation create(NewReservation n) {
        return store.tx(c -> {
            Room room = store.lockRoom(c, n.roomNumber())
                    .orElseThrow(() -> new ApiException(404, "ROOM_NOT_FOUND", "Quarto não encontrado"));
            if (n.guests() > room.capacity()) {
                throw new ApiException(422, "CAPACITY_EXCEEDED",
                        "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes");
            }
            if (store.isRoomBusy(c, room.number(), n.checkIn(), n.checkOut())) {
                throw new ApiException(409, "ROOM_UNAVAILABLE", "Quarto já reservado em alguma das noites");
            }
            Pricing.Quote q = Pricing.quote(room.nightlyRate(), n.checkIn(), n.checkOut(), n.ratePlan());
            Reservation draft = new Reservation(0, room.number(), n.guestName(), n.guestEmail(), n.identification(),
                    n.checkIn(), n.checkOut(), n.guests(), n.ratePlan(), q.nights(), q.subtotal(), q.discount(),
                    q.total(), Status.PENDING, Money.ZERO, null);
            long id = store.insert(c, draft);
            return store.find(c, id, false).orElseThrow();
        });
    }

    public Reservation get(long id) {
        return store.tx(c -> load(c, id, false));
    }

    public Reservation confirm(long id) {
        return store.tx(c -> {
            Reservation r = load(c, id, true);
            requireStatus(r, "confirmar", Status.PENDING);
            store.updateState(c, id, Status.CONFIRMED, r.total(), null);
            Reservation confirmed = load(c, id, false);
            Document doc = Documents.render(confirmed, store.room(c, r.roomNumber()).orElseThrow());
            store.addMessage(c, "EMAIL", confirmed.guestEmail(), Documents.CONFIRMATION_SUBJECT,
                    Documents.confirmationEmailBody(confirmed, doc));
            return confirmed;
        });
    }

    public Reservation cancel(long id) {
        return store.tx(c -> {
            Reservation r = load(c, id, true);
            requireStatus(r, "cancelar", Status.PENDING, Status.CONFIRMED);
            BigDecimal refund = r.ratePlan() == RatePlan.STANDARD ? r.amountPaid() : Money.ZERO;
            store.updateState(c, id, Status.CANCELLED, r.amountPaid(), refund);
            return load(c, id, false);
        });
    }

    public Reservation checkIn(long id) {
        return store.tx(c -> {
            Reservation r = load(c, id, true);
            requireStatus(r, "fazer check-in de", Status.CONFIRMED);
            store.updateState(c, id, Status.CHECKED_IN, r.amountPaid(), r.refund());
            return load(c, id, false);
        });
    }

    public Issued checkOut(long id) {
        return store.tx(c -> {
            Reservation r = load(c, id, true);
            requireStatus(r, "fazer check-out de", Status.CHECKED_IN);
            store.updateState(c, id, Status.CHECKED_OUT, r.amountPaid(), r.refund());
            return issue(c, load(c, id, false));
        });
    }

    /** Segunda via: só existe para reserva paga. */
    public Issued document(long id) {
        return store.tx(c -> {
            Reservation r = load(c, id, false);
            if (!r.status().isPaid()) {
                throw ApiException.notAllowed("Não há comprovante para reserva " + r.status());
            }
            return issue(c, r);
        });
    }

    private Issued issue(java.sql.Connection c, Reservation r) throws java.sql.SQLException {
        Room room = store.room(c, r.roomNumber()).orElseThrow();
        return new Issued(r, room, Documents.render(r, room));
    }

    private Reservation load(java.sql.Connection c, long id, boolean lock) throws java.sql.SQLException {
        Optional<Reservation> r = store.find(c, id, lock);
        return r.orElseThrow(() -> new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva não encontrada"));
    }

    private static void requireStatus(Reservation r, String action, Status... allowed) {
        for (Status s : allowed) if (r.status() == s) return;
        throw ApiException.notAllowed("Não é possível " + action + " uma reserva " + r.status());
    }

    // ---- relatórios

    public String occupancyCsv(LocalDate night) {
        return store.tx(c -> {
            List<Store.Occupied> rows = store.occupancy(c, night);
            int total = store.rooms(c).size();
            StringBuilder sb = new StringBuilder("room,type,reservation,check_in,check_out\n");
            for (Store.Occupied o : rows) {
                sb.append(o.room()).append(',').append(o.type()).append(',').append(o.reservation()).append(',')
                        .append(o.checkIn()).append(',').append(o.checkOut()).append('\n');
            }
            sb.append("total,").append(rows.size()).append('/').append(total).append('\n');
            return sb.toString();
        });
    }

    public String revenueCsv(YearMonth month) {
        return store.tx(c -> {
            StringBuilder sb = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
            BigDecimal sum = Money.ZERO;
            for (Reservation r : store.kept(c, month.atDay(1), month.atEndOfMonth())) {
                BigDecimal kept = r.amountPaid().subtract(r.refund() == null ? Money.ZERO : r.refund());
                sum = sum.add(kept);
                sb.append(r.id()).append(',').append(r.roomNumber()).append(',').append(r.checkIn()).append(',')
                        .append(r.checkOut()).append(',').append(r.status()).append(',').append(kept.toPlainString())
                        .append('\n');
            }
            sb.append("total,").append(sum.toPlainString()).append('\n');
            return sb.toString();
        });
    }
}
