package br.com.mare;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Regras de negócio da pousada; dados em memória. */
public class Hotel {
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final List<Room> rooms = List.of(
            new Room(101, Room.RoomType.STANDARD, 2, Money.of("249.90")),
            new Room(102, Room.RoomType.STANDARD, 2, Money.of("249.90")),
            new Room(201, Room.RoomType.DELUXE, 3, Money.of("389.90")),
            new Room(202, Room.RoomType.DELUXE, 3, Money.of("389.90")),
            new Room(301, Room.RoomType.SUITE, 4, Money.of("579.90")));
    private final Map<Integer, Reservation> reservations = new java.util.LinkedHashMap<>();
    private final List<Message> messages = new ArrayList<>();

    public List<Room> rooms() { return rooms; }

    public synchronized List<Message> messages() { return List.copyOf(messages); }

    public synchronized Reservation create(Integer roomNumber, String name, String email, String cpf,
                                           LocalDate in, LocalDate out, int guests, RatePlan plan,
                                           String companyCode) {
        Room room = rooms.stream().filter(r -> r.number() == roomNumber).findFirst()
                .orElseThrow(() -> new ApiException(404, "ROOM_NOT_FOUND", "Quarto não encontrado"));
        RatePlan.Quote quote = plan.quote(room, in, out, companyCode);
        if (guests > room.capacity()) {
            throw new ApiException(422, "CAPACITY_EXCEEDED", "O quarto comporta no máximo " + room.capacity() + " hóspedes");
        }
        boolean taken = reservations.values().stream().anyMatch(r ->
                r.room.number() == room.number() && r.status != ReservationStatus.CANCELLED
                        && in.isBefore(r.checkOut) && r.checkIn.isBefore(out));
        if (taken) {
            throw new ApiException(409, "ROOM_UNAVAILABLE", "Quarto já reservado em alguma das noites");
        }
        Reservation r = new Reservation(reservations.size() + 1, room, name, email, cpf, in, out, guests,
                plan, companyCode, quote);
        reservations.put(r.id, r);
        return r;
    }

    public synchronized Reservation get(String id) {
        Reservation r = null;
        try {
            r = reservations.get(Integer.parseInt(id));
        } catch (NumberFormatException ignored) {
        }
        if (r == null) throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva não encontrada");
        return r;
    }

    public synchronized Reservation confirm(String id) {
        Reservation r = require(id, ReservationStatus.PENDING, "Só é possível confirmar uma reserva pendente");
        r.status = ReservationStatus.CONFIRMED;
        r.amountPaid = r.total;
        String receipt = receiptText(r);
        String body = "Olá, " + r.guestName + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + receipt + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        messages.add(new Message(messages.size() + 1, "EMAIL", r.guestEmail,
                "Reserva confirmada - Pousada Maré Alta", body));
        return r;
    }

    public synchronized Reservation cancel(String id) {
        Reservation r = get(id);
        if (r.status != ReservationStatus.PENDING && r.status != ReservationStatus.CONFIRMED) {
            throw ApiException.notAllowed("Só é possível cancelar antes do check-in");
        }
        r.refund = r.plan.refund(r.amountPaid);
        r.status = ReservationStatus.CANCELLED;
        return r;
    }

    public synchronized Reservation checkIn(String id) {
        Reservation r = require(id, ReservationStatus.CONFIRMED, "Só é possível fazer check-in de uma reserva confirmada");
        r.status = ReservationStatus.CHECKED_IN;
        return r;
    }

    public synchronized Reservation checkOut(String id) {
        Reservation r = require(id, ReservationStatus.CHECKED_IN, "Só é possível fazer check-out de quem fez check-in");
        r.status = ReservationStatus.CHECKED_OUT;
        return r;
    }

    /** Segunda via: só para reserva paga. */
    public synchronized Reservation paid(String id) {
        Reservation r = get(id);
        if (!r.status.isPaid()) throw ApiException.notAllowed("Comprovante só existe para reserva paga");
        return r;
    }

    private Reservation require(String id, ReservationStatus expected, String msg) {
        Reservation r = get(id);
        if (r.status != expected) throw ApiException.notAllowed(msg);
        return r;
    }

    // ---- comprovante ----

    public static String receiptText(Reservation r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(r.id).append('\n');
        sb.append("Hóspede: ").append(r.guestName).append(" (CPF ").append(r.guestCpf).append(")\n");
        sb.append("Quarto: ").append(r.room.number()).append(" (").append(r.room.type()).append(")\n");
        sb.append("Entrada: ").append(BR_DATE.format(r.checkIn)).append(" | Saída: ").append(BR_DATE.format(r.checkOut))
                .append(" | ").append(r.nights()).append(r.nights() == 1 ? " diária" : " diárias").append('\n');
        sb.append("Diárias: ").append(Money.brl(r.subtotal)).append('\n');
        if (r.discount.signum() > 0) {
            sb.append("Desconto: ").append(Money.brl(r.discount)).append('\n');
        }
        sb.append("Total: ").append(Money.brl(r.total));
        return sb.toString();
    }

    // ---- relatórios ----

    public synchronized String occupancyCsv(LocalDate date) {
        List<Reservation> list = reservations.values().stream()
                .filter(r -> r.status.isPaid() && r.occupies(date))
                .sorted(Comparator.comparingInt((Reservation r) -> r.room.number()).thenComparingInt(r -> r.id))
                .toList();
        StringBuilder sb = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : list) {
            sb.append(r.room.number()).append(',').append(r.room.type()).append(',').append(r.id).append(',')
                    .append(r.checkIn).append(',').append(r.checkOut).append('\n');
        }
        long occupied = list.stream().map(r -> r.room.number()).distinct().count();
        sb.append("total,").append(occupied).append('/').append(rooms.size()).append('\n');
        return sb.toString();
    }

    public synchronized String revenueCsv(YearMonth month) {
        List<Reservation> list = reservations.values().stream()
                .filter(r -> YearMonth.from(r.checkIn).equals(month) && r.netAmount().signum() > 0)
                .sorted(Comparator.comparing((Reservation r) -> r.checkIn).thenComparingInt(r -> r.id))
                .toList();
        StringBuilder sb = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal sum = Money.ZERO;
        for (Reservation r : list) {
            sb.append(r.id).append(',').append(r.room.number()).append(',').append(r.checkIn).append(',')
                    .append(r.checkOut).append(',').append(r.status).append(',').append(r.netAmount()).append('\n');
            sum = sum.add(r.netAmount());
        }
        sb.append("total,").append(Money.round(sum)).append('\n');
        return sb.toString();
    }
}
