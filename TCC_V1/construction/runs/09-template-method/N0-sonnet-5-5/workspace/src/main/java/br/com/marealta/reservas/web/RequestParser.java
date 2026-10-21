package br.com.marealta.reservas.web;

import br.com.marealta.reservas.domain.BookingException;
import br.com.marealta.reservas.domain.ErrorCode;
import br.com.marealta.reservas.domain.RatePlan;
import br.com.marealta.reservas.domain.ReservationService.NewReservation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

final class RequestParser {
    private final ObjectMapper mapper;

    RequestParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    NewReservation parseNewReservation(String body) {
        JsonNode json;
        try {
            json = mapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw invalid("O corpo deve ser um JSON válido");
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

    private static BookingException invalid(String message) {
        return new BookingException(ErrorCode.INVALID_REQUEST, message);
    }

    private static String text(JsonNode json, String field) {
        JsonNode n = json.get(field);
        if (n == null || !n.isTextual() || n.asText().isBlank()) {
            throw invalid("Campo " + field + " é obrigatório");
        }
        return n.asText();
    }

    private static int integer(JsonNode json, String field) {
        JsonNode n = json.get(field);
        if (n == null || !n.isIntegralNumber() || !n.canConvertToInt()) {
            throw invalid("Campo " + field + " é obrigatório e deve ser um número inteiro");
        }
        return n.asInt();
    }

    private static LocalDate date(JsonNode json, String field) {
        try {
            return LocalDate.parse(text(json, field));
        } catch (DateTimeParseException e) {
            throw invalid("Campo " + field + " deve estar no formato AAAA-MM-DD");
        }
    }

    private static RatePlan ratePlan(JsonNode json) {
        try {
            return RatePlan.valueOf(text(json, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw invalid("Campo ratePlan deve ser STANDARD ou NON_REFUNDABLE");
        }
    }
}
