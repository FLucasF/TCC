package pousada;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Corpo validado do POST /reservations. */
public record ReservationRequest(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                 LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {

    public static ReservationRequest parse(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw ApiException.invalidRequest("O corpo deve ser um objeto JSON");
        }
        LocalDate checkIn = date(body, "checkIn");
        LocalDate checkOut = date(body, "checkOut");
        if (!checkOut.isAfter(checkIn)) {
            throw ApiException.invalidRequest("A saída deve ser depois da entrada");
        }
        int guests = integer(body, "guests");
        if (guests < 1) {
            throw ApiException.invalidRequest("É preciso pelo menos um hóspede");
        }
        return new ReservationRequest(integer(body, "roomNumber"), text(body, "guestName"), text(body, "guestEmail"),
                text(body, "guestCpf"), checkIn, checkOut, guests, ratePlan(body));
    }

    private static String text(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw ApiException.invalidRequest("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asText();
    }

    private static int integer(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw ApiException.invalidRequest("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asInt();
    }

    private static LocalDate date(JsonNode body, String field) {
        String value = text(body, field);
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw ApiException.invalidRequest("Data inválida em " + field + " (use AAAA-MM-DD)");
        }
    }

    private static RatePlan ratePlan(JsonNode body) {
        String value = text(body, "ratePlan");
        try {
            return RatePlan.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw ApiException.invalidRequest("Plano de tarifa inválido: " + value);
        }
    }
}
