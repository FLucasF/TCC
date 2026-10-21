package br.com.marealta;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Regras de negócio da pousada, com os dados em memória. */
public class Hotel {
    private static final String CONFIRMATION_SUBJECT = "Reserva confirmada - Pousada Maré Alta";

    private final List<Room> rooms = List.of(
            Room.of(101, RoomType.STANDARD), Room.of(102, RoomType.STANDARD),
            Room.of(201, RoomType.DELUXE), Room.of(202, RoomType.DELUXE),
            Room.of(301, RoomType.SUITE));
    private final Map<Integer, Reservation> reservations = new LinkedHashMap<>();
    private final MessageSender sender;

    public Hotel(MessageSender sender) {
        this.sender = sender;
    }

    public List<Room> rooms() {
        return rooms;
    }

    synchronized Reservation reserve(ReservationRequest request) {
        Room room = rooms.stream().filter(r -> r.number() == request.roomNumber()).findFirst()
                .orElseThrow(() -> DomainException.roomNotFound(request.roomNumber()));
        if (request.guests() > room.capacity()) {
            throw DomainException.capacityExceeded(room.capacity());
        }
        boolean taken = reservations.values().stream().anyMatch(r -> r.isActive()
                && r.room().number() == room.number() && r.overlaps(request.checkIn(), request.checkOut()));
        if (taken) {
            throw DomainException.roomUnavailable();
        }
        Reservation reservation = new Reservation(reservations.size() + 1, room, request.guestName(),
                request.guestEmail(), request.guestCpf(), request.checkIn(), request.checkOut(),
                request.guests(), request.ratePlan());
        reservations.put(reservation.id(), reservation);
        return reservation;
    }

    synchronized Reservation find(String id) {
        Reservation reservation;
        try {
            reservation = reservations.get(Integer.parseInt(id));
        } catch (NumberFormatException e) {
            reservation = null;
        }
        if (reservation == null) {
            throw DomainException.reservationNotFound(id);
        }
        return reservation;
    }

    synchronized Reservation confirm(String id) {
        Reservation reservation = find(id);
        reservation.confirm();
        sender.send(new OutgoingMessage(Channel.EMAIL, reservation.guestEmail(), CONFIRMATION_SUBJECT,
                confirmationBody(reservation)));
        return reservation;
    }

    synchronized Reservation cancel(String id) {
        Reservation reservation = find(id);
        reservation.cancel();
        return reservation;
    }

    synchronized Reservation checkIn(String id) {
        Reservation reservation = find(id);
        reservation.doCheckIn();
        return reservation;
    }

    synchronized Receipt checkOut(String id) {
        Reservation reservation = find(id);
        reservation.doCheckOut();
        return reservation.receipt();
    }

    synchronized Receipt document(String id) {
        return find(id).receipt();
    }

    synchronized String occupancyReport(LocalDate night) {
        List<Reservation> occupying = reservations.values().stream()
                .filter(r -> r.occupiesNight(night))
                .sorted(Comparator.comparingInt(r -> r.room().number()))
                .toList();
        List<String> rows = new ArrayList<>();
        for (Reservation r : occupying) {
            rows.add(String.join(",", String.valueOf(r.room().number()), r.room().type().name(),
                    String.valueOf(r.id()), r.checkIn().toString(), r.checkOut().toString()));
        }
        return csv("room,type,reservation,check_in,check_out", rows, occupying.size() + "/" + rooms.size());
    }

    synchronized String revenueReport(YearMonth month) {
        List<Reservation> kept = reservations.values().stream()
                .filter(r -> YearMonth.from(r.checkIn()).equals(month) && r.netAmount().signum() > 0)
                .sorted(Comparator.comparing(Reservation::checkIn).thenComparingInt(Reservation::id))
                .toList();
        List<String> rows = new ArrayList<>();
        var total = Money.ZERO;
        for (Reservation r : kept) {
            rows.add(String.join(",", String.valueOf(r.id()), String.valueOf(r.room().number()),
                    r.checkIn().toString(), r.checkOut().toString(), r.status().name(),
                    r.netAmount().toPlainString()));
            total = total.add(r.netAmount());
        }
        return csv("reservation,room,check_in,check_out,status,amount", rows, total.toPlainString());
    }

    private static String csv(String header, List<String> rows, String total) {
        StringBuilder out = new StringBuilder(header).append('\n');
        rows.forEach(row -> out.append(row).append('\n'));
        return out.append("total,").append(total).append('\n').toString();
    }

    private static String confirmationBody(Reservation reservation) {
        return "Olá, " + reservation.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + reservation.receipt().toText() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
    }
}
