package pousada;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.databind.JsonNode;

import pousada.identity.IdentityKinds;

/** Valida o corpo do POST /reservations e cria a reserva. */
final class ReservationRequest {
    private ReservationRequest() {
    }

    static Reservation parse(JsonNode body, ReservationService service) {
        int roomNumber = integer(body, "roomNumber");
        String guestName = text(body, "guestName");
        String guestEmail = text(body, "guestEmail");
        LocalDate checkIn = date(body, "checkIn");
        LocalDate checkOut = date(body, "checkOut");
        int guests = integer(body, "guests");
        RatePlan ratePlan = ratePlan(body);
        var identity = IdentityKinds.parse(body);
        if (!checkOut.isAfter(checkIn)) {
            throw ApiException.invalid("A saída deve ser depois da entrada.");
        }
        if (guests < 1) {
            throw ApiException.invalid("Informe pelo menos um hóspede.");
        }
        return service.create(roomNumber, guestName, guestEmail, identity, checkIn, checkOut, guests, ratePlan);
    }

    private static int integer(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || !node.isInt()) {
            throw ApiException.invalid("O campo " + field + " é obrigatório e deve ser um número inteiro.");
        }
        return node.asInt();
    }

    private static String text(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw ApiException.invalid("O campo " + field + " é obrigatório.");
        }
        return node.asText().trim();
    }

    private static LocalDate date(JsonNode body, String field) {
        try {
            return LocalDate.parse(text(body, field));
        } catch (DateTimeParseException e) {
            throw ApiException.invalid("O campo " + field + " deve estar no formato AAAA-MM-DD.");
        }
    }

    private static RatePlan ratePlan(JsonNode body) {
        try {
            return RatePlan.valueOf(text(body, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw ApiException.invalid("ratePlan deve ser STANDARD ou NON_REFUNDABLE.");
        }
    }
}
