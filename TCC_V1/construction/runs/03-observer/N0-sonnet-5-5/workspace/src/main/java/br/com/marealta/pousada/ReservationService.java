package br.com.marealta.pousada;

import br.com.marealta.pousada.domain.*;
import br.com.marealta.pousada.events.EventBus;
import br.com.marealta.pousada.events.ReservationEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ReservationService {
    public record NewReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                 LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
    }

    private final List<Room> rooms;
    private final EventBus events;
    private final List<Reservation> reservations = new ArrayList<>();

    public ReservationService(List<Room> rooms, EventBus events) {
        this.rooms = List.copyOf(rooms);
        this.events = events;
    }

    public static List<Room> defaultRooms() {
        return List.of(
                new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90")),
                new Room(102, RoomType.STANDARD, 2, new BigDecimal("249.90")),
                new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90")),
                new Room(202, RoomType.DELUXE, 3, new BigDecimal("389.90")),
                new Room(301, RoomType.SUITE, 4, new BigDecimal("579.90")));
    }

    public List<Room> rooms() {
        return rooms;
    }

    public synchronized Reservation create(NewReservation n) {
        if (!n.checkOut().isAfter(n.checkIn())) {
            throw invalid("A saída deve ser depois da entrada");
        }
        if (n.guests() < 1) {
            throw invalid("É preciso pelo menos um hóspede");
        }
        Room room = rooms.stream().filter(r -> r.number() == n.roomNumber()).findFirst()
                .orElseThrow(() -> new ApiException(404, "ROOM_NOT_FOUND", "Quarto não encontrado"));
        if (n.guests() > room.capacity()) {
            throw new ApiException(422, "CAPACITY_EXCEEDED",
                    "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes");
        }
        boolean taken = reservations.stream().anyMatch(r -> r.room().equals(room)
                && r.status() != ReservationStatus.CANCELLED && r.overlaps(n.checkIn(), n.checkOut()));
        if (taken) {
            throw new ApiException(409, "ROOM_UNAVAILABLE", "Quarto já reservado em alguma das noites");
        }
        Reservation r = new Reservation(reservations.size() + 1, room, n.guestName(), n.guestEmail(),
                n.guestCpf(), n.checkIn(), n.checkOut(), n.guests(), n.ratePlan());
        reservations.add(r);
        return r;
    }

    public synchronized Reservation get(String id) {
        try {
            int i = Integer.parseInt(id);
            if (i >= 1 && i <= reservations.size()) {
                return reservations.get(i - 1);
            }
        } catch (NumberFormatException ignored) {
            // falls through to not found
        }
        throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva não encontrada");
    }

    public synchronized Reservation confirm(String id) {
        Reservation r = get(id);
        r.confirm();
        events.publish(new ReservationEvent.Confirmed(r));
        return r;
    }

    public synchronized Reservation cancel(String id) {
        Reservation r = get(id);
        boolean wasConfirmed = r.status() == ReservationStatus.CONFIRMED;
        r.cancel();
        if (wasConfirmed) {
            events.publish(new ReservationEvent.CancelledAfterConfirmation(r));
        }
        return r;
    }

    public synchronized Reservation checkIn(String id) {
        Reservation r = get(id);
        r.startStay();
        return r;
    }

    public synchronized Receipt checkOut(String id) {
        Reservation r = get(id);
        r.endStay();
        return Receipt.of(r);
    }

    public synchronized Receipt document(String id) {
        Reservation r = get(id);
        if (!r.status().isPaid()) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED",
                    "Segunda via só existe para reserva paga");
        }
        return Receipt.of(r);
    }

    public synchronized String occupancyCsv(LocalDate night) {
        StringBuilder sb = new StringBuilder("room,type,reservation,check_in,check_out\n");
        List<Reservation> occupying = reservations.stream()
                .filter(r -> r.status().isPaid() && r.occupiesNight(night))
                .sorted(Comparator.comparingInt((Reservation r) -> r.room().number()))
                .toList();
        for (Reservation r : occupying) {
            sb.append(r.room().number()).append(',').append(r.room().type()).append(',')
                    .append(r.id()).append(',').append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
        }
        long rooms = occupying.stream().map(r -> r.room().number()).distinct().count();
        sb.append("total,").append(rooms).append('/').append(this.rooms.size()).append('\n');
        return sb.toString();
    }

    public synchronized String revenueCsv(YearMonth month) {
        StringBuilder sb = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal total = Money.ZERO;
        List<Reservation> rows = reservations.stream()
                .filter(r -> YearMonth.from(r.checkIn()).equals(month) && r.netAmount().signum() > 0)
                .sorted(Comparator.comparing(Reservation::checkIn).thenComparingInt(Reservation::id))
                .toList();
        for (Reservation r : rows) {
            sb.append(r.id()).append(',').append(r.room().number()).append(',').append(r.checkIn()).append(',')
                    .append(r.checkOut()).append(',').append(r.status()).append(',')
                    .append(r.netAmount().toPlainString()).append('\n');
            total = Money.round(total.add(r.netAmount()));
        }
        sb.append("total,").append(total.toPlainString()).append('\n');
        return sb.toString();
    }

    private static ApiException invalid(String message) {
        return new ApiException(400, "INVALID_REQUEST", message);
    }
}
