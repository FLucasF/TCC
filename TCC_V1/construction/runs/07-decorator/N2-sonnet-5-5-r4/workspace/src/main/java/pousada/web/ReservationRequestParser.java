package pousada.web;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.databind.JsonNode;

import pousada.domain.ApiException;
import pousada.domain.RatePlan;
import pousada.service.NewReservation;

/** Valida o corpo do POST /reservations; qualquer defeito vira 400 INVALID_REQUEST. */
final class ReservationRequestParser {
    private ReservationRequestParser() {
    }

    static NewReservation parse(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw ApiException.invalidRequest("O corpo deve ser um objeto JSON");
        }
        int roomNumber = integer(body, "roomNumber");
        String name = text(body, "guestName");
        String email = text(body, "guestEmail");
        String cpf = text(body, "guestCpf");
        LocalDate checkIn = date(body, "checkIn");
        LocalDate checkOut = date(body, "checkOut");
        int guests = integer(body, "guests");
        RatePlan plan = ratePlan(body);
        if (!checkOut.isAfter(checkIn)) {
            throw ApiException.invalidRequest("A saída deve ser depois da entrada");
        }
        if (guests < 1) {
            throw ApiException.invalidRequest("É preciso ao menos um hóspede");
        }
        return new NewReservation(roomNumber, name, email, cpf, checkIn, checkOut, guests, plan);
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
        try {
            return LocalDate.parse(text(body, field));
        } catch (DateTimeParseException e) {
            throw ApiException.invalidRequest("Data inválida em " + field + " (use AAAA-MM-DD)");
        }
    }

    private static RatePlan ratePlan(JsonNode body) {
        try {
            return RatePlan.valueOf(text(body, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw ApiException.invalidRequest("Plano de tarifa inválido");
        }
    }
}
