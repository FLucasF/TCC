package pousada;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.function.UnaryOperator;

class ReservationService {
    private final Database db;
    private final Repository repository;

    ReservationService(Database db, Repository repository) {
        this.db = db;
        this.repository = repository;
    }

    List<Room> rooms() {
        return db.transaction(repository::rooms);
    }

    List<Message> messages() {
        return db.transaction(repository::messages);
    }

    Reservation create(JsonNode body) {
        int roomNumber = integer(body, "roomNumber");
        String guestName = text(body, "guestName");
        String guestEmail = text(body, "guestEmail");
        Identification identification = IdentificationTypes.fromRequest(body);
        LocalDate checkIn = date(body, "checkIn");
        LocalDate checkOut = date(body, "checkOut");
        int guests = integer(body, "guests");
        RatePlan plan = ratePlan(body);
        if (!checkOut.isAfter(checkIn)) {
            throw ApiException.invalid("A saída deve ser depois da entrada.");
        }
        if (guests < 1) {
            throw ApiException.invalid("É preciso ao menos um hóspede.");
        }
        return db.transaction(c -> {
            Room room = repository.findRoomLocked(c, roomNumber).orElseThrow(
                    () -> new ApiException(404, "ROOM_NOT_FOUND", "Quarto " + roomNumber + " não existe."));
            if (guests > room.capacity()) {
                throw new ApiException(422, "CAPACITY_EXCEEDED",
                        "O quarto " + roomNumber + " comporta até " + room.capacity() + " pessoas.");
            }
            if (repository.isRoomBooked(c, roomNumber, checkIn, checkOut)) {
                throw new ApiException(409, "ROOM_UNAVAILABLE",
                        "O quarto " + roomNumber + " já está reservado em alguma dessas noites.");
            }
            Pricing pricing = Pricing.quote(room, checkIn, checkOut, plan);
            BigDecimal zero = Money.round(BigDecimal.ZERO);
            return repository.insert(c, new Reservation(0, room, guestName, guestEmail, identification,
                    checkIn, checkOut, guests, plan, pricing, Status.PENDING, zero, null));
        });
    }

    Reservation get(long id) {
        return db.transaction(c -> repository.find(c, id).orElseThrow(() -> notFound(id)));
    }

    Reservation confirm(long id) {
        return change(id, Reservation::confirm, ReservationService::sendConfirmationEmail);
    }

    Reservation cancel(long id) {
        return change(id, Reservation::cancel, null);
    }

    Reservation checkIn(long id) {
        return change(id, Reservation::doCheckIn, null);
    }

    Reservation checkOut(long id) {
        return change(id, Reservation::doCheckOut, null);
    }

    /** Segunda via: só existe para reserva paga. */
    Reservation paidReservation(long id) {
        Reservation r = get(id);
        requirePaid(r);
        return r;
    }

    private interface AfterChange {
        void run(Repository repository, Connection c, Reservation r) throws SQLException;
    }

    private Reservation change(long id, UnaryOperator<Reservation> transition, AfterChange after) {
        return db.transaction(c -> {
            Reservation changed = transition.apply(repository.findLocked(c, id).orElseThrow(() -> notFound(id)));
            repository.saveState(c, changed);
            if (after != null) {
                after.run(repository, c, changed);
            }
            return changed;
        });
    }

    private static void sendConfirmationEmail(Repository repository, Connection c, Reservation r)
            throws SQLException {
        String document = r.identification().type().document().render(r);
        String body = "Olá, " + r.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + document + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        repository.saveMessage(c, "EMAIL", r.guestEmail(), "Reserva confirmada - Pousada Maré Alta", body);
    }

    private static void requirePaid(Reservation r) {
        if (!r.status().isPaid()) {
            throw ApiException.notAllowed("A reserva está " + r.status() + " e não tem documento.");
        }
    }

    private static ApiException notFound(long id) {
        return new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva " + id + " não existe.");
    }

    private static JsonNode required(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || node.isNull()) {
            throw ApiException.invalid("Campo obrigatório ausente: " + field);
        }
        return node;
    }

    private static int integer(JsonNode body, String field) {
        JsonNode node = required(body, field);
        if (!node.isIntegralNumber() || !node.canConvertToInt()) {
            throw ApiException.invalid("Campo inválido: " + field);
        }
        return node.asInt();
    }

    private static String text(JsonNode body, String field) {
        JsonNode node = required(body, field);
        if (!node.isTextual() || node.asText().isBlank()) {
            throw ApiException.invalid("Campo inválido: " + field);
        }
        return node.asText().trim();
    }

    private static LocalDate date(JsonNode body, String field) {
        try {
            return LocalDate.parse(text(body, field));
        } catch (java.time.format.DateTimeParseException e) {
            throw ApiException.invalid("Data inválida em " + field);
        }
    }

    private static RatePlan ratePlan(JsonNode body) {
        try {
            return RatePlan.valueOf(text(body, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw ApiException.invalid("Plano de tarifa inválido.");
        }
    }
}
