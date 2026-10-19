package br.com.mareal.domain;

import br.com.mareal.plans.Pricing;
import br.com.mareal.plans.RatePlan;
import br.com.mareal.plans.RatePlans;
import br.com.mareal.plans.Stay;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class ReservationService {
    /** Booking data already checked for format by the API layer. */
    public record NewReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                 LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan,
                                 String companyCode) {}

    private final List<Room> rooms = List.of(
            new Room(101, RoomType.STANDARD), new Room(102, RoomType.STANDARD),
            new Room(201, RoomType.DELUXE), new Room(202, RoomType.DELUXE),
            new Room(301, RoomType.SUITE));
    private final List<Reservation> reservations = new ArrayList<>();
    private final Outbox outbox;
    private final Clock clock;

    public ReservationService(Outbox outbox, Clock clock) {
        this.outbox = outbox;
        this.clock = clock;
    }

    public List<Room> rooms() {
        return rooms;
    }

    public synchronized Reservation create(NewReservation req) {
        Room room = rooms.stream().filter(r -> r.number() == req.roomNumber()).findFirst()
                .orElseThrow(() -> new ApiException(404, "ROOM_NOT_FOUND", "Quarto " + req.roomNumber() + " não existe."));
        if (req.guests() > room.capacity()) {
            throw new ApiException(422, "CAPACITY_EXCEEDED",
                    "O quarto " + room.number() + " comporta até " + room.capacity() + " pessoas.");
        }
        RatePlan plan = RatePlans.find(req.ratePlan())
                .orElseThrow(() -> ApiException.invalid("Plano de tarifa desconhecido: " + req.ratePlan()));
        Stay stay = new Stay(room.type(), req.checkIn(), req.checkOut(), req.companyCode(), LocalDate.now(clock));
        Pricing pricing = plan.price(stay);
        boolean taken = reservations.stream().anyMatch(r -> r.room.equals(room)
                && r.status != ReservationStatus.CANCELLED && r.overlaps(req.checkIn(), req.checkOut()));
        if (taken) {
            throw new ApiException(409, "ROOM_UNAVAILABLE", "O quarto " + room.number() + " já está reservado nesse período.");
        }
        Reservation reservation = new Reservation(reservations.size() + 1, room, req.guestName(), req.guestEmail(),
                req.guestCpf(), req.checkIn(), req.checkOut(), req.guests(), plan, stay.nights(),
                pricing.subtotal(), pricing.discount(), pricing.total());
        reservations.add(reservation);
        return reservation;
    }

    public synchronized Reservation get(String id) {
        try {
            int index = Integer.parseInt(id);
            if (index >= 1 && index <= reservations.size()) {
                return reservations.get(index - 1);
            }
        } catch (NumberFormatException ignored) {
            // falls through to not found
        }
        throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva " + id + " não existe.");
    }

    public synchronized Reservation confirm(String id) {
        Reservation r = get(id);
        require(r, r.status == ReservationStatus.PENDING, "confirmar");
        r.status = ReservationStatus.CONFIRMED;
        r.amountPaid = r.total;
        Receipt receipt = Receipt.of(r);
        outbox.sendEmail(r.guestEmail, "Reserva confirmada - Pousada Maré Alta",
                "Olá, " + r.guestName + "!\n\n"
                        + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                        + receipt.text() + "\n\n"
                        + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                        + "Até breve!");
        return r;
    }

    public synchronized Reservation checkIn(String id) {
        Reservation r = get(id);
        require(r, r.status == ReservationStatus.CONFIRMED, "fazer check-in de");
        r.status = ReservationStatus.CHECKED_IN;
        return r;
    }

    public synchronized Receipt checkOut(String id) {
        Reservation r = get(id);
        require(r, r.status == ReservationStatus.CHECKED_IN, "fazer check-out de");
        r.status = ReservationStatus.CHECKED_OUT;
        return Receipt.of(r);
    }

    public synchronized Reservation cancel(String id) {
        Reservation r = get(id);
        require(r, r.status == ReservationStatus.PENDING || r.status == ReservationStatus.CONFIRMED, "cancelar");
        r.refund = Money.round(r.ratePlan.refund(r.amountPaid));
        r.status = ReservationStatus.CANCELLED;
        return r;
    }

    public synchronized Receipt document(String id) {
        Reservation r = get(id);
        require(r, r.status.isPaid(), "emitir a segunda via de");
        return Receipt.of(r);
    }

    public synchronized String occupancyReport(LocalDate night) {
        return Reports.occupancy(night, List.copyOf(reservations), rooms);
    }

    public synchronized String revenueReport(YearMonth month) {
        return Reports.revenue(month, List.copyOf(reservations));
    }

    private static void require(Reservation r, boolean allowed, String action) {
        if (!allowed) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED",
                    "Não é possível " + action + " a reserva " + r.id + " com status " + r.status + ".");
        }
    }
}
