package br.com.marealta.http;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.marealta.domain.DomainException;
import br.com.marealta.service.NewReservation;

/** Converte o que chega pelo HTTP em valores do domínio, recusando o que está malformado. */
final class RequestParser {
    private RequestParser() {
    }

    static NewReservation newReservation(ObjectMapper mapper, String body) {
        JsonNode json;
        try {
            json = mapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw DomainException.invalid("O corpo não é um JSON válido");
        }
        if (json == null || !json.isObject()) {
            throw DomainException.invalid("O corpo deve ser um objeto JSON");
        }
        JsonNode company = json.get("companyCode");
        return new NewReservation(
                integer(json, "roomNumber"),
                text(json, "guestName"),
                text(json, "guestEmail"),
                text(json, "guestCpf"),
                date(text(json, "checkIn"), "checkIn"),
                date(text(json, "checkOut"), "checkOut"),
                integer(json, "guests"),
                text(json, "ratePlan"),
                company != null && company.isTextual() ? company.asText() : null);
    }

    static LocalDate date(String value, String field) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw DomainException.invalid("Data inválida em " + field + " (use AAAA-MM-DD)");
        }
    }

    static YearMonth month(String value) {
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw DomainException.invalid("Mês inválido (use AAAA-MM)");
        }
    }

    private static String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw DomainException.invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asText();
    }

    private static int integer(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw DomainException.invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asInt();
    }
}
