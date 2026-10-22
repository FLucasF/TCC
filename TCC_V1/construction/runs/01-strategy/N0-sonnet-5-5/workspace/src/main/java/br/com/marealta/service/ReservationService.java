package br.com.marealta.service;

import br.com.marealta.ApiException;
import br.com.marealta.db.Database;
import br.com.marealta.db.Repository;
import br.com.marealta.model.*;
import br.com.marealta.plan.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ReservationService {
    public record NewReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                 LocalDate checkIn, LocalDate checkOut, int guests, RatePlan plan, String companyCode) {}

    private final Database db;

    public ReservationService(Database db) { this.db = db; }

    public List<Room> rooms() {
        return db.inTransaction(Repository::rooms);
    }

    public List<Message> messages() {
        return db.inTransaction(Repository::messages);
    }

    public Reservation create(NewReservation n) {
        return db.inTransaction(c -> {
            Room room = Repository.lockRoom(c, n.roomNumber())
                    .orElseThrow(() -> new ApiException(404, "ROOM_NOT_FOUND", "Quarto " + n.roomNumber() + " não existe"));
            Quote quote = n.plan().quote(new QuoteRequest(room, n.checkIn(), n.checkOut(), n.companyCode()));
            if (n.guests() > room.capacity()) {
                throw new ApiException(422, "CAPACITY_EXCEEDED",
                        "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes");
            }
            if (Repository.hasConflict(c, room.number(), n.checkIn(), n.checkOut())) {
                throw new ApiException(409, "ROOM_UNAVAILABLE", "O quarto já está reservado em alguma das noites");
            }
            String company = n.plan() instanceof CorporatePlan ? n.companyCode() : null;
            Reservation draft = new Reservation(0, room.number(), n.guestName(), n.guestEmail(), n.guestCpf(),
                    n.checkIn(), n.checkOut(), n.guests(), n.plan().code(), company, quote.nights(),
                    quote.subtotal(), quote.discount(), quote.total(), ReservationStatus.PENDING,
                    Money.round(BigDecimal.ZERO), null);
            long id = Repository.insert(c, draft);
            return Repository.reservation(c, id, false).orElseThrow();
        });
    }

    public Reservation get(long id) {
        return db.inTransaction(c -> load(c, id, false));
    }

    public Reservation confirm(long id) {
        return db.inTransaction(c -> {
            Reservation r = load(c, id, true);
            require(r, r.status() == ReservationStatus.PENDING, "Só é possível confirmar uma reserva pendente");
            Repository.updateState(c, id, ReservationStatus.CONFIRMED, r.total(), null);
            Reservation confirmed = load(c, id, false);
            Receipt receipt = receipt(c, confirmed);
            Repository.insertMessage(c, "EMAIL", confirmed.guestEmail(), "Reserva confirmada - Pousada Maré Alta",
                    "Olá, " + confirmed.guestName() + "!\n\n"
                            + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                            + receipt.text() + "\n\n"
                            + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                            + "Até breve!");
            return confirmed;
        });
    }

    public Reservation cancel(long id) {
        return db.inTransaction(c -> {
            Reservation r = load(c, id, true);
            require(r, r.status() == ReservationStatus.PENDING || r.status() == ReservationStatus.CONFIRMED,
                    "Só é possível cancelar antes do check-in");
            RatePlan plan = planOf(r);
            BigDecimal refund = Money.round(plan.refund(r.amountPaid()));
            Repository.updateState(c, id, ReservationStatus.CANCELLED, r.amountPaid(), refund);
            return load(c, id, false);
        });
    }

    public Reservation checkIn(long id) {
        return db.inTransaction(c -> {
            Reservation r = load(c, id, true);
            require(r, r.status() == ReservationStatus.CONFIRMED, "Só é possível fazer check-in de uma reserva confirmada");
            Repository.updateState(c, id, ReservationStatus.CHECKED_IN, r.amountPaid(), r.refund());
            return load(c, id, false);
        });
    }

    public Receipt checkOut(long id) {
        return db.inTransaction(c -> {
            Reservation r = load(c, id, true);
            require(r, r.status() == ReservationStatus.CHECKED_IN, "Só é possível fazer check-out após o check-in");
            Repository.updateState(c, id, ReservationStatus.CHECKED_OUT, r.amountPaid(), r.refund());
            return receipt(c, load(c, id, false));
        });
    }

    public Receipt document(long id) {
        return db.inTransaction(c -> {
            Reservation r = load(c, id, false);
            require(r, r.status().isPaid(), "A segunda via só existe para reserva paga");
            return receipt(c, r);
        });
    }

    private static Reservation load(Connection c, long id, boolean lock) throws SQLException {
        return Repository.reservation(c, id, lock)
                .orElseThrow(() -> new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva " + id + " não existe"));
    }

    private static void require(Reservation r, boolean ok, String message) {
        if (!ok) throw ApiException.notAllowed(message + " (situação atual: " + r.status() + ")");
    }

    private static RatePlan planOf(Reservation r) {
        return RatePlans.find(r.ratePlan()).orElseThrow(() -> new IllegalStateException("Plano desconhecido: " + r.ratePlan()));
    }

    private static Receipt receipt(Connection c, Reservation r) throws SQLException {
        Room room = Repository.room(c, r.roomNumber()).orElseThrow();
        return new Receipt(r.id(), r.guestName(), r.guestCpf(), room.number(), room.type(), r.checkIn(), r.checkOut(),
                r.nights(), r.subtotal(), r.discount(), r.total());
    }
}
