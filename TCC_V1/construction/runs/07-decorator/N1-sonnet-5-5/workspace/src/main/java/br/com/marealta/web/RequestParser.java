package br.com.marealta.web;

import br.com.marealta.domain.DomainException;
import br.com.marealta.domain.ErrorCode;
import br.com.marealta.domain.Guest;
import br.com.marealta.domain.NewReservation;
import br.com.marealta.domain.RatePlan;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/** Lê e valida o que chega do site; qualquer problema vira INVALID_REQUEST. */
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
            throw invalid("O corpo não é um JSON válido");
        }
        if (json == null || !json.isObject()) {
            throw invalid("O corpo deve ser um objeto JSON");
        }
        LocalDate checkIn = date(json, "checkIn");
        LocalDate checkOut = date(json, "checkOut");
        if (!checkOut.isAfter(checkIn)) {
            throw invalid("A saída deve ser depois da entrada");
        }
        int guests = integer(json, "guests");
        if (guests < 1) {
            throw invalid("É preciso pelo menos um hóspede");
        }
        return new NewReservation(integer(json, "roomNumber"),
                new Guest(text(json, "guestName"), text(json, "guestEmail"), text(json, "guestCpf")),
                checkIn, checkOut, guests, ratePlan(json));
    }

    LocalDate day(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw invalid("Data inválida, use o formato 2027-07-10");
        }
    }

    YearMonth month(String value) {
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw invalid("Mês inválido, use o formato 2027-07");
        }
    }

    private String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asText();
    }

    private int integer(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asInt();
    }

    private LocalDate date(JsonNode json, String field) {
        return day(text(json, field));
    }

    private RatePlan ratePlan(JsonNode json) {
        try {
            return RatePlan.valueOf(text(json, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw invalid("Plano de tarifa inválido");
        }
    }

    private DomainException invalid(String message) {
        return new DomainException(ErrorCode.INVALID_REQUEST, message);
    }
}
