package pousada.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pousada.domain.ApiException;
import pousada.domain.ErrorCode;
import pousada.domain.NewReservation;
import pousada.domain.RatePlan;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Lê e valida o corpo do POST /reservations. */
final class ReservationRequestParser {
    private final ObjectMapper mapper;

    ReservationRequestParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    NewReservation parse(String body) {
        JsonNode json;
        try {
            json = mapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw invalid("O corpo não é um JSON válido");
        }
        if (json == null || !json.isObject()) {
            throw invalid("O corpo deve ser um objeto JSON");
        }
        LocalDate checkIn = date(json, "checkIn");
        LocalDate checkOut = date(json, "checkOut");
        if (!checkOut.isAfter(checkIn)) {
            throw invalid("A saída deve ser depois da entrada");
        }
        int guests = integer(json, "guests");
        if (guests < 1) {
            throw invalid("É preciso pelo menos um hóspede");
        }
        return new NewReservation(integer(json, "roomNumber"), text(json, "guestName"),
                text(json, "guestEmail"), text(json, "guestCpf"), checkIn, checkOut, guests, ratePlan(json));
    }

    private static String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asText();
    }

    private static int integer(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asInt();
    }

    private static LocalDate date(JsonNode json, String field) {
        String text = text(json, field);
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw invalid("Data inválida em " + field + ": " + text);
        }
    }

    private static RatePlan ratePlan(JsonNode json) {
        String text = text(json, "ratePlan");
        try {
            return RatePlan.valueOf(text);
        } catch (IllegalArgumentException e) {
            throw invalid("Plano de tarifa inválido: " + text);
        }
    }

    private static ApiException invalid(String message) {
        return new ApiException(ErrorCode.INVALID_REQUEST, message);
    }
}
