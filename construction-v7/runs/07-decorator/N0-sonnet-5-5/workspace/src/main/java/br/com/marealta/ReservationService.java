package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ReservationService {
    public static final List<Room> ROOMS = List.of(
            new Room(101, RoomType.STANDARD, 2, Money.of("249.90")),
            new Room(102, RoomType.STANDARD, 2, Money.of("249.90")),
            new Room(201, RoomType.DELUXE, 3, Money.of("389.90")),
            new Room(202, RoomType.DELUXE, 3, Money.of("389.90")),
            new Room(301, RoomType.SUITE, 4, Money.of("579.90")));

    public record CreateCommand(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {}

    private final NotificationService notifications;
    private final List<Reservation> reservations = new ArrayList<>();

    public ReservationService(NotificationService notifications) {
        this.notifications = notifications;
    }

    public List<Room> rooms() { return ROOMS; }

    public Optional<Room> findRoom(int number) {
        return ROOMS.stream().filter(r -> r.number() == number).findFirst();
    }

    public synchronized Reservation create(CreateCommand c) {
        if (!c.checkOut().isAfter(c.checkIn())) throw ApiException.invalid("A saída deve ser depois da entrada");
        if (c.guests() < 1) throw ApiException.invalid("É preciso pelo menos um hóspede");
        Room room = findRoom(c.roomNumber()).orElseThrow(() -> ApiException.roomNotFound(c.roomNumber()));
        if (c.guests() > room.capacity()) {
            throw ApiException.capacity("O quarto " + room.number() + " comporta no máximo " + room.capacity() + " pessoas");
        }
        boolean taken = reservations.stream().anyMatch(r -> r.room.number() == room.number()
                && r.status != ReservationStatus.CANCELLED && r.overlaps(c.checkIn(), c.checkOut()));
        if (taken) throw ApiException.unavailable("O quarto " + room.number() + " já está reservado em alguma das noites");
        Pricing.Quote q = Pricing.quote(room, c.checkIn(), c.checkOut(), c.ratePlan());
        Reservation r = new Reservation(reservations.size() + 1, room, c.guestName(), c.guestEmail(),
                c.guestCpf(), c.checkIn(), c.checkOut(), c.guests(), c.ratePlan(), q);
        reservations.add(r);
        return r;
    }

    public synchronized Reservation get(String id) {
        int n;
        try {
            n = Integer.parseInt(id);
        } catch (NumberFormatException e) {
            throw ApiException.reservationNotFound(id);
        }
        if (n < 1 || n > reservations.size()) throw ApiException.reservationNotFound(id);
        return reservations.get(n - 1);
    }

    public synchronized Reservation confirm(String id) {
        Reservation r = get(id);
        require(r, ReservationStatus.PENDING, "Só é possível confirmar uma reserva pendente");
        r.status = ReservationStatus.CONFIRMED;
        r.amountPaid = r.total;
        notifications.sendEmail(r.guestEmail, Receipt.confirmationSubject(), Receipt.confirmationBody(r));
        return r;
    }

    public synchronized Reservation checkIn(String id) {
        Reservation r = get(id);
        require(r, ReservationStatus.CONFIRMED, "Só é possível fazer check-in de uma reserva confirmada");
        r.status = ReservationStatus.CHECKED_IN;
        return r;
    }

    public synchronized Reservation checkOut(String id) {
        Reservation r = get(id);
        require(r, ReservationStatus.CHECKED_IN, "Só é possível fazer check-out de uma reserva com check-in");
        r.status = ReservationStatus.CHECKED_OUT;
        return r;
    }

    public synchronized Reservation cancel(String id) {
        Reservation r = get(id);
        if (r.status != ReservationStatus.PENDING && r.status != ReservationStatus.CONFIRMED) {
            throw ApiException.notAllowed("Só é possível cancelar uma reserva pendente ou confirmada");
        }
        r.refund = (r.status == ReservationStatus.CONFIRMED && r.ratePlan == RatePlan.STANDARD)
                ? r.amountPaid : Money.round(BigDecimal.ZERO);
        r.status = ReservationStatus.CANCELLED;
        return r;
    }

    /** Reserva paga, para a segunda via do comprovante. */
    public synchronized Reservation paidReservation(String id) {
        Reservation r = get(id);
        if (!r.status.isPaid()) {
            throw ApiException.notAllowed("O comprovante só existe para reserva paga");
        }
        return r;
    }

    private void require(Reservation r, ReservationStatus expected, String message) {
        if (r.status != expected) throw ApiException.notAllowed(message);
    }

    // ---- relatórios ----

    public synchronized String occupancyCsv(LocalDate night) {
        List<Reservation> occupied = reservations.stream()
                .filter(r -> r.status.isPaid() && !night.isBefore(r.checkIn) && night.isBefore(r.checkOut))
                .sorted(Comparator.comparingInt((Reservation r) -> r.room.number()).thenComparingInt(r -> r.id))
                .toList();
        StringBuilder sb = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : occupied) {
            sb.append(r.room.number()).append(',').append(r.room.type()).append(',').append(r.id).append(',')
                    .append(r.checkIn).append(',').append(r.checkOut).append('\n');
        }
        long rooms = occupied.stream().map(r -> r.room.number()).distinct().count();
        sb.append("total,").append(rooms).append('/').append(ROOMS.size()).append('\n');
        return sb.toString();
    }

    public synchronized String revenueCsv(YearMonth month) {
        List<Reservation> list = reservations.stream()
                .filter(r -> YearMonth.from(r.checkIn).equals(month) && r.netAmount().signum() > 0)
                .sorted(Comparator.comparing((Reservation r) -> r.checkIn).thenComparingInt(r -> r.id))
                .toList();
        StringBuilder sb = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal sum = Money.round(BigDecimal.ZERO);
        for (Reservation r : list) {
            sb.append(r.id).append(',').append(r.room.number()).append(',').append(r.checkIn).append(',')
                    .append(r.checkOut).append(',').append(r.status).append(',')
                    .append(r.netAmount().toPlainString()).append('\n');
            sum = sum.add(r.netAmount());
        }
        sb.append("total,").append(Money.round(sum).toPlainString()).append('\n');
        return sb.toString();
    }
}
