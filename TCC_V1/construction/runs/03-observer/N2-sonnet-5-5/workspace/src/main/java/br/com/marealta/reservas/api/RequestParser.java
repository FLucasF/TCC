package br.com.marealta.reservas.api;

import br.com.marealta.reservas.domain.DomainException;
import br.com.marealta.reservas.domain.ErrorCode;
import br.com.marealta.reservas.domain.RatePlan;
import br.com.marealta.reservas.service.NewReservation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/** Valida o que chega do site e transforma em comandos do sistema; qualquer defeito vira INVALID_REQUEST. */
final class RequestParser {
    private final ObjectMapper mapper;

    RequestParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    NewReservation newReservation(String body) {
        JsonNode json;
        try {
            json = mapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw invalid("O corpo precisa ser um JSON válido");
        }
        if (json == null || !json.isObject()) {
            throw invalid("O corpo precisa ser um objeto JSON");
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

    LocalDate date(String value, String field) {
        if (value == null) {
            throw invalid("Informe " + field + " no formato aaaa-mm-dd");
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw invalid("Data inválida em " + field + ": use o formato aaaa-mm-dd");
        }
    }

    YearMonth month(String value) {
        if (value == null) {
            throw invalid("Informe month no formato aaaa-mm");
        }
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw invalid("Mês inválido: use o formato aaaa-mm");
        }
    }

    private RatePlan ratePlan(String value) {
        try {
            return RatePlan.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw invalid("ratePlan inválido: use STANDARD ou NON_REFUNDABLE");
        }
    }

    private String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw invalid("Informe " + field);
        }
        return node.asText();
    }

    private int integer(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw invalid("Informe " + field + " como número inteiro");
        }
        return node.asInt();
    }

    private static DomainException invalid(String message) {
        return new DomainException(ErrorCode.INVALID_REQUEST, message);
    }
}
