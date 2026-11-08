package br.com.marealta.pousada.web;

import br.com.marealta.pousada.domain.RatePlan;
import br.com.marealta.pousada.service.NewReservation;
import br.com.marealta.pousada.service.ServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Lê o corpo do POST /reservations, recusando com INVALID_REQUEST o que estiver faltando ou mal formado. */
class ReservationRequestReader {

    private final ObjectMapper mapper = new ObjectMapper();

    NewReservation read(String body) {
        JsonNode json;
        try {
            json = mapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw ServiceException.invalid("O corpo não é um JSON válido");
        }
        if (json == null || !json.isObject()) {
            throw ServiceException.invalid("O corpo deve ser um objeto JSON");
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
            throw ServiceException.invalid("Campo " + field + " ausente ou inválido");
        }
        return node.asText();
    }

    private static int integer(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw ServiceException.invalid("Campo " + field + " ausente ou inválido");
        }
        return node.asInt();
    }

    private static LocalDate date(JsonNode json, String field) {
        try {
            return LocalDate.parse(text(json, field));
        } catch (DateTimeParseException e) {
            throw ServiceException.invalid("Campo " + field + " deve estar no formato AAAA-MM-DD");
        }
    }

    private static RatePlan ratePlan(JsonNode json) {
        try {
            return RatePlan.valueOf(text(json, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw ServiceException.invalid("Campo ratePlan deve ser STANDARD ou NON_REFUNDABLE");
        }
    }
}
