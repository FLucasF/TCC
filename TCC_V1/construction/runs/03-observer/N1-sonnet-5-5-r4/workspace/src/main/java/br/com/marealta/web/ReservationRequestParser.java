package br.com.marealta.web;

import br.com.marealta.app.CreateReservation;
import br.com.marealta.domain.DomainException;
import br.com.marealta.domain.ErrorCode;
import br.com.marealta.domain.RatePlan;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Lê e valida o corpo do POST /reservations; qualquer problema é INVALID_REQUEST. */
final class ReservationRequestParser {
    private final ObjectMapper mapper;

    ReservationRequestParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    CreateReservation parse(String body) {
        JsonNode json;
        try {
            json = mapper.readTree(body);
        } catch (Exception e) {
            throw invalid("O corpo não é um JSON válido");
        }
        if (json == null || !json.isObject()) {
            throw invalid("O corpo deve ser um objeto JSON");
        }
        LocalDate checkIn = date(json, "checkIn");
        LocalDate checkOut = date(json, "checkOut");
        int guests = integer(json, "guests");
        if (!checkOut.isAfter(checkIn)) {
            throw invalid("A saída deve ser depois da entrada");
        }
        if (guests < 1) {
            throw invalid("É preciso pelo menos um hóspede");
        }
        String email = text(json, "guestEmail");
        if (!email.contains("@")) {
            throw invalid("guestEmail inválido");
        }
        return new CreateReservation(integer(json, "roomNumber"), text(json, "guestName"), email,
                text(json, "guestCpf"), checkIn, checkOut, guests, ratePlan(json));
    }

    private static String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw invalid("Campo " + field + " ausente ou inválido");
        }
        return node.asText().trim();
    }

    private static int integer(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw invalid("Campo " + field + " ausente ou inválido");
        }
        return node.asInt();
    }

    private static LocalDate date(JsonNode json, String field) {
        try {
            return LocalDate.parse(text(json, field));
        } catch (DateTimeParseException e) {
            throw invalid("Campo " + field + " deve ter o formato AAAA-MM-DD");
        }
    }

    private static RatePlan ratePlan(JsonNode json) {
        try {
            return RatePlan.valueOf(text(json, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw invalid("Campo ratePlan inválido");
        }
    }

    static DomainException invalid(String message) {
        return new DomainException(ErrorCode.INVALID_REQUEST, message);
    }
}
