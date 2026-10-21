package com.pousadamaretalta;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Regras de negócio, com os dados em memória. Todas as operações são serializadas. */
public class ReservationService {
    private final Map<Integer, Reservation> reservations = new TreeMap<>();
    private final List<Message> outbox = new ArrayList<>();
    private int nextReservationId = 1;

    public List<Room> rooms() {
        return Room.ALL;
    }

    public synchronized Reservation create(JsonNode body) {
        String guestName = Json.requiredText(body, "guestName");
        String guestEmail = Json.requiredText(body, "guestEmail");
        GuestIdentity identity = IdentityReader.read(body);
        int roomNumber = Json.requiredInt(body, "roomNumber");
        LocalDate checkIn = Json.requiredDate(body, "checkIn");
        LocalDate checkOut = Json.requiredDate(body, "checkOut");
        int guests = Json.requiredInt(body, "guests");
        RatePlan plan;
        try {
            plan = RatePlan.valueOf(Json.requiredText(body, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw ApiException.invalid("ratePlan deve ser STANDARD ou NON_REFUNDABLE");
        }
        if (!checkOut.isAfter(checkIn)) throw ApiException.invalid("A saída deve ser depois da entrada");
        if (guests < 1) throw ApiException.invalid("É preciso pelo menos um hóspede");

        Room room = Room.ALL.stream().filter(r -> r.number() == roomNumber).findFirst()
                .orElseThrow(() -> new ApiException(404, "ROOM_NOT_FOUND", "Quarto " + roomNumber + " não existe"));
        if (guests > room.capacity()) {
            throw new ApiException(422, "CAPACITY_EXCEEDED",
                    "O quarto " + roomNumber + " comporta no máximo " + room.capacity() + " hóspedes");
        }
        boolean taken = reservations.values().stream()
                .anyMatch(r -> r.room.equals(room) && r.status != ReservationStatus.CANCELLED
                        && r.overlaps(checkIn, checkOut));
        if (taken) {
            throw new ApiException(409, "ROOM_UNAVAILABLE", "O quarto " + roomNumber + " já está reservado nesse período");
        }

        Reservation r = new Reservation(nextReservationId++, room, guestName, guestEmail, identity,
                checkIn, checkOut, guests, plan, Pricing.quote(room, checkIn, checkOut, plan));
        reservations.put(r.id, r);
        return r;
    }

    public synchronized Reservation get(String id) {
        return find(id);
    }

    public synchronized Reservation confirm(String id) {
        Reservation r = find(id);
        require(r, r.status == ReservationStatus.PENDING, "Só é possível confirmar uma reserva pendente");
        r.status = ReservationStatus.CONFIRMED;
        r.amountPaid = r.quote.total();
        ReservationDocument doc = r.document();
        String body = "Olá, " + r.guestName + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + doc.text() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        outbox.add(new Message(outbox.size() + 1, "EMAIL", r.guestEmail,
                "Reserva confirmada - Pousada Maré Alta", body));
        return r;
    }

    public synchronized Reservation cancel(String id) {
        Reservation r = find(id);
        require(r, r.status == ReservationStatus.PENDING || r.status == ReservationStatus.CONFIRMED,
                "Só é possível cancelar antes do check-in");
        boolean refunds = r.status == ReservationStatus.CONFIRMED && r.ratePlan.refundable;
        r.refund = refunds ? r.amountPaid : Money.ZERO;
        r.status = ReservationStatus.CANCELLED;
        return r;
    }

    public synchronized Reservation checkIn(String id) {
        Reservation r = find(id);
        require(r, r.status == ReservationStatus.CONFIRMED, "O check-in exige uma reserva confirmada");
        r.status = ReservationStatus.CHECKED_IN;
        return r;
    }

    public synchronized ReservationDocument checkOut(String id) {
        Reservation r = find(id);
        require(r, r.status == ReservationStatus.CHECKED_IN, "O check-out exige uma reserva com check-in");
        r.status = ReservationStatus.CHECKED_OUT;
        return r.document();
    }

    public synchronized ReservationDocument document(String id) {
        Reservation r = find(id);
        require(r, r.status.isPaid(), "Só há documento para reserva paga");
        return r.document();
    }

    public synchronized List<Message> messages() {
        return List.copyOf(outbox);
    }

    public synchronized String occupancyCsv(String date) {
        LocalDate night;
        try {
            if (date == null) throw new IllegalArgumentException();
            night = LocalDate.parse(date);
        } catch (RuntimeException e) {
            throw ApiException.invalid("O parâmetro date deve estar no formato AAAA-MM-DD");
        }
        StringBuilder sb = new StringBuilder("room,type,reservation,check_in,check_out\n");
        List<Reservation> rows = reservations.values().stream()
                .filter(r -> r.status.isPaid() && r.occupies(night))
                .sorted(Comparator.comparingInt((Reservation r) -> r.room.number()).thenComparingInt(r -> r.id))
                .toList();
        for (Reservation r : rows) {
            sb.append(r.room.number()).append(',').append(r.room.type()).append(',').append(r.id).append(',')
                    .append(r.checkIn).append(',').append(r.checkOut).append('\n');
        }
        long occupied = rows.stream().map(r -> r.room.number()).distinct().count();
        sb.append("total,").append(occupied).append('/').append(Room.ALL.size()).append('\n');
        return sb.toString();
    }

    public synchronized String revenueCsv(String month) {
        YearMonth ym;
        try {
            if (month == null) throw new IllegalArgumentException();
            ym = YearMonth.parse(month);
        } catch (RuntimeException e) {
            throw ApiException.invalid("O parâmetro month deve estar no formato AAAA-MM");
        }
        StringBuilder sb = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        List<Reservation> rows = reservations.values().stream()
                .filter(r -> YearMonth.from(r.checkIn).equals(ym) && r.netAmount().signum() > 0)
                .sorted(Comparator.comparing((Reservation r) -> r.checkIn).thenComparingInt(r -> r.id))
                .toList();
        BigDecimal total = Money.ZERO;
        for (Reservation r : rows) {
            sb.append(r.id).append(',').append(r.room.number()).append(',').append(r.checkIn).append(',')
                    .append(r.checkOut).append(',').append(r.status).append(',').append(r.netAmount()).append('\n');
            total = Money.round(total.add(r.netAmount()));
        }
        sb.append("total,").append(total).append('\n');
        return sb.toString();
    }

    private Reservation find(String id) {
        Reservation r = null;
        try {
            r = reservations.get(Integer.parseInt(id));
        } catch (NumberFormatException ignored) {
        }
        if (r == null) throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva " + id + " não existe");
        return r;
    }

    private static void require(Reservation r, boolean ok, String message) {
        if (!ok) throw ApiException.notAllowed(message + " (situação atual: " + r.status + ")");
    }
}
