package br.com.marealta.web;

import br.com.marealta.domain.ErrorCode;
import br.com.marealta.domain.PousadaException;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.service.NewReservation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

final class RequestParser {
    private final ObjectMapper mapper;

    RequestParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    NewReservation reservation(String body) {
        JsonNode json;
        try {
            json = mapper.readTree(body);
        } catch (Exception e) {
            throw invalid("Body is not valid JSON");
        }
        if (json == null || !json.isObject()) {
            throw invalid("Body must be a JSON object");
        }
        LocalDate checkIn = date(json, "checkIn");
        LocalDate checkOut = date(json, "checkOut");
        int guests = integer(json, "guests");
        if (!checkOut.isAfter(checkIn)) {
            throw invalid("checkOut must be after checkIn");
        }
        if (guests < 1) {
            throw invalid("guests must be at least 1");
        }
        return new NewReservation(integer(json, "roomNumber"), text(json, "guestName"), text(json, "guestEmail"),
                text(json, "guestCpf"), checkIn, checkOut, guests, ratePlan(json));
    }

    private static String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw invalid(field + " is required");
        }
        return node.asText().strip();
    }

    private static int integer(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw invalid(field + " must be an integer");
        }
        return node.intValue();
    }

    private static LocalDate date(JsonNode json, String field) {
        String raw = text(json, field);
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw invalid(field + " must be formatted as yyyy-MM-dd");
        }
    }

    private static RatePlan ratePlan(JsonNode json) {
        String raw = text(json, "ratePlan");
        try {
            return RatePlan.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw invalid("ratePlan must be STANDARD or NON_REFUNDABLE");
        }
    }

    private static PousadaException invalid(String message) {
        return new PousadaException(ErrorCode.INVALID_REQUEST, message);
    }
}
