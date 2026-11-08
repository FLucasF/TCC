package br.com.marealta.web;

import br.com.marealta.DomainException;
import br.com.marealta.service.ReservationRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

final class RequestParser {
    private RequestParser() {}

    static ReservationRequest reservation(ObjectMapper mapper, String body) {
        JsonNode json;
        try {
            json = mapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw DomainException.invalid("Corpo da requisição não é um JSON válido");
        }
        if (json == null || !json.isObject()) throw DomainException.invalid("Corpo da requisição deve ser um objeto JSON");

        JsonNode roomNode = json.get("roomNumber");
        JsonNode guestsNode = json.get("guests");
        if (roomNode == null || !roomNode.isIntegralNumber()) throw DomainException.invalid("roomNumber é obrigatório");
        if (guestsNode == null || !guestsNode.isIntegralNumber()) throw DomainException.invalid("guests é obrigatório");
        int guests = guestsNode.asInt();
        if (guests < 1) throw DomainException.invalid("É preciso pelo menos um hóspede");

        LocalDate checkIn = date(json, "checkIn");
        LocalDate checkOut = date(json, "checkOut");
        if (!checkOut.isAfter(checkIn)) throw DomainException.invalid("A saída deve ser depois da entrada");

        JsonNode company = json.get("companyCode");
        String companyCode = company != null && company.isTextual() ? company.asText() : null;

        return new ReservationRequest(roomNode.asInt(), text(json, "guestName"), text(json, "guestEmail"),
                text(json, "guestCpf"), checkIn, checkOut, guests, text(json, "ratePlan"), companyCode);
    }

    static LocalDate date(String value, String field) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw DomainException.invalid(field + " inválida, use o formato AAAA-MM-DD");
        }
    }

    static YearMonth month(String value) {
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw DomainException.invalid("month inválido, use o formato AAAA-MM");
        }
    }

    private static LocalDate date(JsonNode json, String field) {
        return date(text(json, field), field);
    }

    private static String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw DomainException.invalid(field + " é obrigatório");
        }
        return node.asText();
    }
}
