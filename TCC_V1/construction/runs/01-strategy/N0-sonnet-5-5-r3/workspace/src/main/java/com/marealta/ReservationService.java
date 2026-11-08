package com.marealta;

import com.marealta.plan.BookingTerms;
import com.marealta.plan.Quote;
import com.marealta.plan.RatePlan;
import com.marealta.plan.RatePlans;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public final class ReservationService {
    public static final String EMAIL_SUBJECT = "Reserva confirmada - Pousada Maré Alta";

    public record NewReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                 LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan,
                                 String companyCode) {}

    private final Db db;

    public ReservationService(Db db) {
        this.db = db;
    }

    public List<Room> rooms() {
        return db.tx(Store::rooms);
    }

    public List<Message> messages() {
        return db.tx(Store::messages);
    }

    public Reservation create(NewReservation n) {
        RatePlan plan = RatePlans.find(n.ratePlan())
                .orElseThrow(() -> ApiException.invalid("Plano de tarifa desconhecido: " + n.ratePlan()));
        return db.tx(c -> {
            Room room = Store.lockRoom(c, n.roomNumber())
                    .orElseThrow(() -> new ApiException(404, "ROOM_NOT_FOUND", "Quarto não encontrado: " + n.roomNumber()));
            Quote q = plan.quote(new BookingTerms(room, n.checkIn(), n.checkOut(), n.companyCode()));
            if (n.guests() > room.capacity()) {
                throw new ApiException(422, "CAPACITY_EXCEEDED",
                        "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes");
            }
            if (Store.hasConflict(c, room.number(), n.checkIn(), n.checkOut())) {
                throw new ApiException(409, "ROOM_UNAVAILABLE", "O quarto já está reservado em alguma das noites");
            }
            String company = "CORPORATE".equals(plan.code()) ? n.companyCode().trim() : null;
            return Store.insert(c, new Reservation(0, room.number(), n.guestName(), n.guestEmail(), n.guestCpf(),
                    n.checkIn(), n.checkOut(), n.guests(), plan.code(), company, q.nights(), q.subtotal(), q.discount(),
                    q.total(), ReservationStatus.PENDING, Money.ZERO, null));
        });
    }

    public Reservation get(long id) {
        return db.tx(c -> load(c, id, false));
    }

    public Reservation confirm(long id) {
        return db.tx(c -> {
            Reservation r = load(c, id, true);
            require(r, r.status() == ReservationStatus.PENDING, "confirmar");
            Reservation paid = withState(r, ReservationStatus.CONFIRMED, r.total(), null);
            Store.updateState(c, paid);
            Receipt receipt = receipt(c, paid);
            Store.insertMessage(c, new Message(0, "EMAIL", paid.guestEmail(), EMAIL_SUBJECT,
                    "Olá, " + paid.guestName() + "!\n\n"
                            + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                            + receipt.text() + "\n\n"
                            + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                            + "Até breve!"));
            return paid;
        });
    }

    public Reservation cancel(long id) {
        return db.tx(c -> {
            Reservation r = load(c, id, true);
            require(r, r.status() == ReservationStatus.PENDING || r.status() == ReservationStatus.CONFIRMED, "cancelar");
            Reservation done = withState(r, ReservationStatus.CANCELLED, r.amountPaid(),
                    RatePlans.require(r.ratePlan()).refund(r.amountPaid()));
            Store.updateState(c, done);
            return done;
        });
    }

    public Reservation checkIn(long id) {
        return db.tx(c -> {
            Reservation r = load(c, id, true);
            require(r, r.status() == ReservationStatus.CONFIRMED, "fazer check-in");
            Reservation done = withState(r, ReservationStatus.CHECKED_IN, r.amountPaid(), r.refund());
            Store.updateState(c, done);
            return done;
        });
    }

    public Receipt checkOut(long id) {
        return db.tx(c -> {
            Reservation r = load(c, id, true);
            require(r, r.status() == ReservationStatus.CHECKED_IN, "fazer check-out");
            Reservation done = withState(r, ReservationStatus.CHECKED_OUT, r.amountPaid(), r.refund());
            Store.updateState(c, done);
            return receipt(c, done);
        });
    }

    public Receipt document(long id) {
        return db.tx(c -> {
            Reservation r = load(c, id, false);
            if (!r.status().isPaid()) {
                throw new ApiException(409, "OPERATION_NOT_ALLOWED",
                        "Só existe comprovante para reserva paga (situação atual: " + r.status() + ")");
            }
            return receipt(c, r);
        });
    }

    public String occupancyReport(LocalDate night) {
        return db.tx(c -> Reports.occupancy(Store.rooms(c), Store.coveringNight(c, night)));
    }

    public String revenueReport(YearMonth month) {
        return db.tx(c -> Reports.revenue(Store.startingBetween(c, month.atDay(1), month.plusMonths(1).atDay(1))));
    }

    private static Reservation load(Connection c, long id, boolean lock) throws SQLException {
        return Store.find(c, id, lock).orElseThrow(
                () -> new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva não encontrada: " + id));
    }

    private static void require(Reservation r, boolean allowed, String action) {
        if (!allowed) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED",
                    "Não é possível " + action + " uma reserva na situação " + r.status());
        }
    }

    private static Receipt receipt(Connection c, Reservation r) throws SQLException {
        Room room = Store.rooms(c).stream().filter(x -> x.number() == r.roomNumber()).findFirst().orElseThrow();
        return Receipt.of(r, room);
    }

    private static Reservation withState(Reservation r, ReservationStatus status, java.math.BigDecimal paid,
                                         java.math.BigDecimal refund) {
        return new Reservation(r.id(), r.roomNumber(), r.guestName(), r.guestEmail(), r.guestCpf(), r.checkIn(),
                r.checkOut(), r.guests(), r.ratePlan(), r.companyCode(), r.nights(), r.subtotal(), r.discount(),
                r.total(), status, paid, refund);
    }
}
