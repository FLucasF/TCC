package br.com.marealta.web;

import br.com.marealta.domain.DomainException;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.service.NewReservation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/** Valida o corpo e os parâmetros vindos do site; qualquer problema vira INVALID_REQUEST. */
public final class RequestParser {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RequestParser() {}

    public static NewReservation reservation(String body) {
        JsonNode json;
        try {
            json = MAPPER.readTree(body);
        } catch (Exception e) {
            throw DomainException.invalidRequest("O corpo não é um JSON válido");
        }
        if (json == null || !json.isObject()) {
            throw DomainException.invalidRequest("O corpo deve ser um objeto JSON");
        }
        return new NewReservation(
                integer(json, "roomNumber"),
                text(json, "guestName"),
                text(json, "guestEmail"),
                text(json, "guestCpf"),
                date(text(json, "checkIn"), "checkIn"),
                date(text(json, "checkOut"), "checkOut"),
                integer(json, "guests"),
                ratePlan(text(json, "ratePlan")));
    }

    public static LocalDate date(String value, String field) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw DomainException.invalidRequest("Data inválida em " + field + " (use AAAA-MM-DD)");
        }
    }

    public static YearMonth month(String value) {
        try {
            if (value == null || !value.matches("\\d{4}-\\d{2}")) throw new DateTimeParseException("", "", 0);
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw DomainException.invalidRequest("Mês inválido (use AAAA-MM)");
        }
    }

    private static String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw DomainException.invalidRequest("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asText();
    }

    private static int integer(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw DomainException.invalidRequest("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.intValue();
    }

    private static RatePlan ratePlan(String value) {
        try {
            return RatePlan.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw DomainException.invalidRequest("Plano de tarifa inválido: " + value);
        }
    }
}
