package pousada.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pousada.domain.DomainException;
import pousada.service.CreateReservation;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/** Valida o formato do que chega pela API. */
final class RequestParser {
    private RequestParser() {
    }

    static CreateReservation createReservation(ObjectMapper mapper, String body) {
        JsonNode json;
        try {
            json = mapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw DomainException.invalidRequest("O corpo não é um JSON válido.");
        }
        if (json == null || !json.isObject()) {
            throw DomainException.invalidRequest("O corpo deve ser um objeto JSON.");
        }
        LocalDate checkIn = date(json, "checkIn");
        LocalDate checkOut = date(json, "checkOut");
        if (!checkOut.isAfter(checkIn)) {
            throw DomainException.invalidRequest("A saída deve ser depois da entrada.");
        }
        int guests = integer(json, "guests");
        if (guests < 1) {
            throw DomainException.invalidRequest("É preciso ao menos um hóspede.");
        }
        return new CreateReservation(
                integer(json, "roomNumber"),
                text(json, "guestName"),
                text(json, "guestEmail"),
                text(json, "guestCpf"),
                checkIn,
                checkOut,
                guests,
                text(json, "ratePlan"),
                optionalText(json, "companyCode"));
    }

    static LocalDate date(String name, String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw DomainException.invalidRequest("Parâmetro " + name + " deve estar no formato AAAA-MM-DD.");
        }
    }

    static YearMonth month(String value) {
        try {
            if (value == null || !value.matches("\\d{4}-\\d{2}")) {
                throw new DateTimeParseException("formato", String.valueOf(value), 0);
            }
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw DomainException.invalidRequest("Parâmetro month deve estar no formato AAAA-MM.");
        }
    }

    private static String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw DomainException.invalidRequest("Campo " + field + " é obrigatório.");
        }
        return node.asText();
    }

    private static String optionalText(JsonNode json, String field) {
        JsonNode node = json.get(field);
        return node != null && node.isTextual() ? node.asText().trim() : null;
    }

    private static int integer(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw DomainException.invalidRequest("Campo " + field + " é obrigatório e deve ser um número inteiro.");
        }
        return node.asInt();
    }

    private static LocalDate date(JsonNode json, String field) {
        return date(field, text(json, field));
    }
}
