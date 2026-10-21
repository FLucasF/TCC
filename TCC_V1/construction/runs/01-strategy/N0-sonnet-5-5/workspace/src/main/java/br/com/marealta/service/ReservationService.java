package br.com.marealta.service;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Quote;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.domain.Room;
import br.com.marealta.domain.RoomType;
import br.com.marealta.plan.BookingRequest;
import br.com.marealta.plan.PlanRuleException;
import br.com.marealta.plan.RatePlan;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Regras de negócio das reservas, em memória. */
public class ReservationService {
    public record NewReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                 LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                                 String companyCode) {}

    private final Map<Integer, Room> rooms = new LinkedHashMap<>();
    private final Map<Integer, Reservation> reservations = new LinkedHashMap<>();
    private final List<Message> messages = new ArrayList<>();
    private int nextReservationId = 1;
    private int nextMessageId = 1;

    public ReservationService() {
        for (Room r : List.of(
                new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90")),
                new Room(102, RoomType.STANDARD, 2, new BigDecimal("249.90")),
                new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90")),
                new Room(202, RoomType.DELUXE, 3, new BigDecimal("389.90")),
                new Room(301, RoomType.SUITE, 4, new BigDecimal("579.90")))) {
            rooms.put(r.number(), r);
        }
    }

    public synchronized List<Room> rooms() {
        return List.copyOf(rooms.values());
    }

    public synchronized List<Message> messages() {
        return List.copyOf(messages);
    }

    public synchronized Reservation create(NewReservation n) {
        if (!n.checkOut().isAfter(n.checkIn())) {
            throw ApiException.invalid("A saída deve ser depois da entrada");
        }
        if (n.guests() < 1) {
            throw ApiException.invalid("É preciso pelo menos um hóspede");
        }
        Room room = rooms.get(n.roomNumber());
        if (room == null) {
            throw new ApiException(404, "ROOM_NOT_FOUND", "Quarto não encontrado");
        }
        Quote quote;
        try {
            quote = n.ratePlan().quote(new BookingRequest(room, n.checkIn(), n.checkOut(), n.companyCode()));
        } catch (PlanRuleException e) {
            throw ApiException.invalid(e.getMessage());
        }
        if (n.guests() > room.capacity()) {
            throw new ApiException(422, "CAPACITY_EXCEEDED", "O quarto comporta no máximo " + room.capacity() + " hóspedes");
        }
        boolean taken = reservations.values().stream().anyMatch(r ->
                r.room().number() == room.number()
                        && r.status() != ReservationStatus.CANCELLED
                        && r.overlaps(n.checkIn(), n.checkOut()));
        if (taken) {
            throw new ApiException(409, "ROOM_UNAVAILABLE", "Quarto já reservado em alguma das noites");
        }
        Reservation res = new Reservation(nextReservationId++, room, n.guestName(), n.guestEmail(), n.guestCpf(),
                n.checkIn(), n.checkOut(), n.guests(), n.ratePlan(), quote);
        reservations.put(res.id(), res);
        return res;
    }

    public synchronized Reservation get(int id) {
        Reservation r = reservations.get(id);
        if (r == null) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva não encontrada");
        }
        return r;
    }

    public synchronized Reservation confirm(int id) {
        Reservation r = require(id, "confirmar", ReservationStatus.PENDING);
        r.confirm();
        messages.add(new Message(nextMessageId++, "EMAIL", r.guestEmail(),
                "Reserva confirmada - Pousada Maré Alta", Receipt.confirmationEmailBody(r)));
        return r;
    }

    public synchronized Reservation checkIn(int id) {
        Reservation r = require(id, "fazer check-in", ReservationStatus.CONFIRMED);
        r.startStay();
        return r;
    }

    /** Faz o check-out e devolve a reserva. */
    public synchronized Reservation checkOut(int id) {
        Reservation r = require(id, "fazer check-out", ReservationStatus.CHECKED_IN);
        r.finishStay();
        return r;
    }

    public synchronized Reservation cancel(int id) {
        Reservation r = require(id, "cancelar", ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
        r.cancel();
        return r;
    }

    /** Segunda via: só para reserva paga. */
    public synchronized Reservation paidReservation(int id) {
        Reservation r = get(id);
        if (!r.status().isPaid()) {
            throw ApiException.notAllowed("Só reservas pagas têm comprovante");
        }
        return r;
    }

    public String receiptText(Reservation r) { return Receipt.text(r); }

    public synchronized String occupancyCsv(LocalDate date) {
        List<Reservation> occupying = reservations.values().stream()
                .filter(r -> r.status().isPaid() && !r.checkIn().isAfter(date) && date.isBefore(r.checkOut()))
                .sorted(Comparator.comparingInt((Reservation r) -> r.room().number()).thenComparingInt(Reservation::id))
                .toList();
        StringBuilder sb = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : occupying) {
            sb.append(r.room().number()).append(',').append(r.room().type()).append(',').append(r.id()).append(',')
                    .append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
        }
        long occupiedRooms = occupying.stream().map(r -> r.room().number()).distinct().count();
        sb.append("total,").append(occupiedRooms).append('/').append(rooms.size()).append('\n');
        return sb.toString();
    }

    public synchronized String revenueCsv(java.time.YearMonth month) {
        List<Reservation> list = reservations.values().stream()
                .filter(r -> java.time.YearMonth.from(r.checkIn()).equals(month)
                        && r.netRevenue().signum() > 0)
                .sorted(Comparator.comparing(Reservation::checkIn).thenComparingInt(Reservation::id))
                .toList();
        BigDecimal total = Money.ZERO;
        StringBuilder sb = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        for (Reservation r : list) {
            sb.append(r.id()).append(',').append(r.room().number()).append(',').append(r.checkIn()).append(',')
                    .append(r.checkOut()).append(',').append(r.status()).append(',')
                    .append(Money.plain(r.netRevenue())).append('\n');
            total = total.add(r.netRevenue());
        }
        sb.append("total,").append(Money.plain(total)).append('\n');
        return sb.toString();
    }

    private Reservation require(int id, String action, ReservationStatus... allowed) {
        Reservation r = get(id);
        for (ReservationStatus s : allowed) {
            if (r.status() == s) return r;
        }
        throw ApiException.notAllowed("Não é possível " + action + " uma reserva com situação " + r.status());
    }
}
