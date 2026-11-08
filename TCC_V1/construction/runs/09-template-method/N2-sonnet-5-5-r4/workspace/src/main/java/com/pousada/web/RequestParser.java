package com.pousada.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousada.domain.DomainException;
import com.pousada.domain.ErrorCode;
import com.pousada.domain.RatePlan;
import com.pousada.service.NewReservation;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Lê o corpo do POST /reservations e recusa o que estiver faltando ou mal formado. */
final class RequestParser {
    private RequestParser() {
    }

    static NewReservation parse(ObjectMapper mapper, String body) {
        JsonNode json;
        try {
            json = mapper.readTree(body);
        } catch (Exception e) {
            throw invalid("O corpo não é um JSON válido");
        }
        if (json == null || !json.isObject()) {
            throw invalid("O corpo deve ser um objeto JSON");
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
            throw invalid("Campo '" + field + "' é obrigatório");
        }
        return node.asText().trim();
    }

    private static int integer(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw invalid("Campo '" + field + "' deve ser um número inteiro");
        }
        return node.asInt();
    }

    private static LocalDate date(JsonNode json, String field) {
        try {
            return LocalDate.parse(text(json, field));
        } catch (DateTimeParseException e) {
            throw invalid("Campo '" + field + "' deve estar no formato 2027-07-10");
        }
    }

    private static RatePlan ratePlan(JsonNode json) {
        try {
            return RatePlan.valueOf(text(json, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw invalid("Campo 'ratePlan' deve ser STANDARD ou NON_REFUNDABLE");
        }
    }

    private static DomainException invalid(String message) {
        return new DomainException(ErrorCode.INVALID_REQUEST, message);
    }
}
