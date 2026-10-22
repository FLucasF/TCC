package br.com.marealta.pousada;

import br.com.marealta.pousada.Domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class ReservationService {
    public record NewReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                 LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {}

    private final Db db;
    private final MailDispatcher mail;

    public ReservationService(Db db, MailDispatcher mail) {
        this.db = db;
        this.mail = mail;
    }

    public List<Room> rooms() {
        return db.read(Repo::rooms);
    }

    public Reservation create(NewReservation n) {
        return db.write(repo -> {
            Room room = repo.room(n.roomNumber())
                    .orElseThrow(() -> new ApiException(404, "ROOM_NOT_FOUND", "Quarto não encontrado"));
            if (n.guests() > room.capacity()) {
                throw new ApiException(422, "CAPACITY_EXCEEDED",
                        "O quarto " + room.number() + " comporta até " + room.capacity() + " pessoas");
            }
            if (repo.hasOverlap(room.number(), n.checkIn(), n.checkOut())) {
                throw new ApiException(409, "ROOM_UNAVAILABLE", "Quarto já reservado em alguma das noites");
            }
            Pricing.Quote q = Pricing.quote(room, n.checkIn(), n.checkOut(), n.ratePlan());
            return repo.insert(new Reservation(0, room.number(), n.guestName(), n.guestEmail(), n.guestCpf(),
                    n.checkIn(), n.checkOut(), n.guests(), n.ratePlan(), q.nights(), q.subtotal(), q.discount(),
                    q.total(), Status.PENDING, Pricing.money(BigDecimal.ZERO), null));
        });
    }

    public Reservation get(long id) {
        return db.read(repo -> load(repo, id));
    }

    public Reservation confirm(long id) {
        return db.write(repo -> {
            Reservation r = require(load(repo, id), Status.PENDING);
            Reservation paid = repo.updateState(id, Status.CONFIRMED, r.total(), null);
            mail.send(repo, paid.guestEmail(), "Reserva confirmada - Pousada Maré Alta", confirmationBody(repo, paid));
            return paid;
        });
    }

    public Reservation cancel(long id) {
        return db.write(repo -> {
            Reservation r = load(repo, id);
            if (r.status() != Status.PENDING && r.status() != Status.CONFIRMED) throw notAllowed(r);
            BigDecimal refund = r.status() == Status.CONFIRMED && r.ratePlan() == RatePlan.STANDARD
                    ? r.amountPaid() : Pricing.money(BigDecimal.ZERO);
            return repo.updateState(id, Status.CANCELLED, r.amountPaid(), refund);
        });
    }

    public Reservation checkIn(long id) {
        return db.write(repo -> {
            Reservation r = require(load(repo, id), Status.CONFIRMED);
            return repo.updateState(id, Status.CHECKED_IN, r.amountPaid(), r.refund());
        });
    }

    public Document checkOut(long id) {
        return db.write(repo -> {
            Reservation r = require(load(repo, id), Status.CHECKED_IN);
            return document(repo, repo.updateState(id, Status.CHECKED_OUT, r.amountPaid(), r.refund()));
        });
    }

    public Document document(long id) {
        return db.read(repo -> {
            Reservation r = load(repo, id);
            if (!r.paid()) throw notAllowed(r);
            return document(repo, r);
        });
    }

    public String occupancyReport(LocalDate night) {
        return db.read(repo -> {
            List<Room> rooms = repo.rooms();
            List<Reservation> occupied = repo.occupying(night);
            StringBuilder sb = new StringBuilder("room,type,reservation,check_in,check_out\n");
            for (Reservation r : occupied) {
                RoomType type = rooms.stream().filter(x -> x.number() == r.roomNumber()).findFirst().orElseThrow().type();
                sb.append(r.roomNumber()).append(',').append(type).append(',').append(r.id()).append(',')
                        .append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
            }
            long roomsOccupied = occupied.stream().map(Reservation::roomNumber).distinct().count();
            return sb.append("total,").append(roomsOccupied).append('/').append(rooms.size()).append('\n').toString();
        });
    }

    public String revenueReport(YearMonth month) {
        return db.read(repo -> {
            List<Reservation> list = repo.revenueStartingBetween(month.atDay(1), month.plusMonths(1).atDay(1));
            StringBuilder sb = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
            BigDecimal sum = Pricing.money(BigDecimal.ZERO);
            for (Reservation r : list) {
                BigDecimal kept = r.amountPaid().subtract(r.refund() == null ? BigDecimal.ZERO : r.refund());
                sum = sum.add(kept);
                sb.append(r.id()).append(',').append(r.roomNumber()).append(',').append(r.checkIn()).append(',')
                        .append(r.checkOut()).append(',').append(r.status()).append(',')
                        .append(kept.toPlainString()).append('\n');
            }
            return sb.append("total,").append(sum.toPlainString()).append('\n').toString();
        });
    }

    public List<Message> messages() {
        return db.read(Repo::messages);
    }

    // ---- internos

    private static Reservation load(Repo repo, long id) {
        return repo.reservation(id)
                .orElseThrow(() -> new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva não encontrada"));
    }

    private static Reservation require(Reservation r, Status expected) {
        if (r.status() != expected) throw notAllowed(r);
        return r;
    }

    private static ApiException notAllowed(Reservation r) {
        return new ApiException(409, "OPERATION_NOT_ALLOWED",
                "Operação não permitida para reserva com situação " + r.status());
    }

    private static Document document(Repo repo, Reservation r) {
        Room room = repo.room(r.roomNumber()).orElseThrow();
        return new Document("Comprovante de reserva", r.id(), r.guestName(), r.guestCpf(), room.number(),
                room.type(), r.checkIn(), r.checkOut(), r.nights(), r.subtotal(), r.discount(), r.total());
    }

    private static String confirmationBody(Repo repo, Reservation r) {
        return "Olá, " + r.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + receiptText(document(repo, r)) + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
    }

    static String receiptText(Document d) {
        var f = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(d.reservationId()).append('\n')
                .append("Hóspede: ").append(d.guestName()).append(" (CPF ").append(d.guestCpf()).append(")\n")
                .append("Quarto: ").append(d.roomNumber()).append(" (").append(d.roomType()).append(")\n")
                .append("Entrada: ").append(d.checkIn().format(f)).append(" | Saída: ").append(d.checkOut().format(f))
                .append(" | ").append(d.nights()).append(d.nights() == 1 ? " diária" : " diárias").append('\n')
                .append("Diárias: ").append(Pricing.brl(d.subtotal())).append('\n');
        if (d.discount().signum() > 0) sb.append("Desconto: ").append(Pricing.brl(d.discount())).append('\n');
        return sb.append("Total: ").append(Pricing.brl(d.total())).toString();
    }
}
