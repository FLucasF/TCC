package pousada.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import pousada.ApiException;
import pousada.document.DocumentTypes;
import pousada.domain.RatePlan;
import pousada.service.NewReservation;

/** Valida o que vem do site; qualquer problema vira 400 INVALID_REQUEST. */
final class RequestParser {
    private final ObjectMapper json;

    RequestParser(ObjectMapper json) {
        this.json = json;
    }

    NewReservation reservation(String body) {
        JsonNode node;
        try {
            node = json.readTree(body);
        } catch (JsonProcessingException e) {
            throw ApiException.invalid("O corpo não é um JSON válido");
        }
        if (node == null || !node.isObject()) {
            throw ApiException.invalid("O corpo precisa ser um objeto JSON");
        }
        Map<String, String> identityFields = new HashMap<>();
        for (String field : DocumentTypes.allFields()) {
            identityFields.put(field, optionalText(node, field));
        }
        LocalDate checkIn = date(node, "checkIn");
        LocalDate checkOut = date(node, "checkOut");
        if (!checkOut.isAfter(checkIn)) {
            throw ApiException.invalid("checkOut precisa ser depois de checkIn");
        }
        int guests = integer(node, "guests");
        if (guests < 1) {
            throw ApiException.invalid("guests precisa ser pelo menos 1");
        }
        return new NewReservation(
                integer(node, "roomNumber"),
                requiredText(node, "guestName"),
                requiredText(node, "guestEmail"),
                DocumentTypes.resolve(identityFields),
                checkIn, checkOut, guests, ratePlan(node));
    }

    static LocalDate date(String text, String name) {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException | NullPointerException e) {
            throw ApiException.invalid(name + " precisa estar no formato 2027-07-10");
        }
    }

    static YearMonth month(String text) {
        try {
            return YearMonth.parse(text);
        } catch (DateTimeParseException | NullPointerException e) {
            throw ApiException.invalid("month precisa estar no formato 2027-07");
        }
    }

    private static LocalDate date(JsonNode node, String field) {
        return date(requiredText(node, field), field);
    }

    private static String requiredText(JsonNode node, String field) {
        String value = optionalText(node, field);
        if (value == null) {
            throw ApiException.invalid("Campo obrigatório ausente: " + field);
        }
        return value;
    }

    private static String optionalText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isTextual()) {
            throw ApiException.invalid(field + " precisa ser um texto");
        }
        return value.asText().isBlank() ? null : value.asText().trim();
    }

    private static int integer(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            throw ApiException.invalid("Campo obrigatório ausente: " + field);
        }
        if (!value.isIntegralNumber() || !value.canConvertToInt()) {
            throw ApiException.invalid(field + " precisa ser um número inteiro");
        }
        return value.asInt();
    }

    private static RatePlan ratePlan(JsonNode node) {
        try {
            return RatePlan.valueOf(requiredText(node, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw ApiException.invalid("ratePlan precisa ser STANDARD ou NON_REFUNDABLE");
        }
    }
}
