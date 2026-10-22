package br.com.marealta.web;

import br.com.marealta.domain.ApiException;
import br.com.marealta.domain.NewReservation;
import br.com.marealta.domain.RatePlan;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Lê e confere o corpo do POST /reservations. */
final class ReservationRequest {
    private ReservationRequest() {}

    static NewReservation parse(ObjectMapper mapper, String body) {
        JsonNode json;
        try {
            json = mapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw ApiException.invalidRequest("O corpo não é um JSON válido");
        }
        if (json == null || !json.isObject()) {
            throw ApiException.invalidRequest("O corpo precisa ser um objeto JSON");
        }
        return new NewReservation(
                integer(json, "roomNumber"),
                text(json, "guestName"),
                text(json, "guestEmail"),
                text(json, "guestCpf"),
                date(json, "checkIn"),
                date(json, "checkOut"),
                integer(json, "guests"),
                ratePlan(json));
    }

    private static String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw ApiException.invalidRequest("Campo '" + field + "' ausente ou inválido");
        }
        return node.asText();
    }

    private static int integer(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw ApiException.invalidRequest("Campo '" + field + "' ausente ou inválido");
        }
        return node.asInt();
    }

    private static LocalDate date(JsonNode json, String field) {
        try {
            return LocalDate.parse(text(json, field));
        } catch (DateTimeParseException e) {
            throw ApiException.invalidRequest("Campo '" + field + "' deve estar no formato aaaa-mm-dd");
        }
    }

    private static RatePlan ratePlan(JsonNode json) {
        try {
            return RatePlan.valueOf(text(json, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw ApiException.invalidRequest("Campo 'ratePlan' deve ser STANDARD ou NON_REFUNDABLE");
        }
    }
}
