package br.com.marealta.pousada.web;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.marealta.pousada.domain.DomainException;
import br.com.marealta.pousada.domain.Guest;
import br.com.marealta.pousada.domain.NewReservation;
import br.com.marealta.pousada.domain.RatePlan;

import static br.com.marealta.pousada.domain.DomainException.Kind.INVALID_REQUEST;

final class RequestParser {

    private final ObjectMapper mapper;

    RequestParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    NewReservation reservation(String body) {
        JsonNode json;
        try {
            json = mapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw invalid("o corpo não é um JSON válido");
        }
        if (json == null || !json.isObject()) {
            throw invalid("o corpo deve ser um objeto JSON");
        }
        return new NewReservation(
                integer(json, "roomNumber"),
                new Guest(text(json, "guestName"), text(json, "guestEmail"), text(json, "guestCpf")),
                date(text(json, "checkIn"), "checkIn"),
                date(text(json, "checkOut"), "checkOut"),
                integer(json, "guests"),
                ratePlan(text(json, "ratePlan")));
    }

    LocalDate date(String value, String field) {
        if (value == null || value.isBlank()) {
            throw invalid("falta o campo " + field);
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw invalid("data inválida em " + field);
        }
    }

    YearMonth month(String value) {
        if (value == null || value.isBlank()) {
            throw invalid("falta o parâmetro month");
        }
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw invalid("mês inválido");
        }
    }

    private RatePlan ratePlan(String value) {
        try {
            return RatePlan.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw invalid("plano de tarifa inválido");
        }
    }

    private String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw invalid("campo ausente ou inválido: " + field);
        }
        return node.asText();
    }

    private int integer(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw invalid("campo ausente ou inválido: " + field);
        }
        return node.asInt();
    }

    private DomainException invalid(String message) {
        return new DomainException(INVALID_REQUEST, message);
    }
}
