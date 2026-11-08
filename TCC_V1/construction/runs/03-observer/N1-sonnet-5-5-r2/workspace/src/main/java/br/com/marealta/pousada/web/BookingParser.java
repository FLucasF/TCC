package br.com.marealta.pousada.web;

import br.com.marealta.pousada.app.BookingRequest;
import br.com.marealta.pousada.domain.ErrorCode;
import br.com.marealta.pousada.domain.PousadaException;
import br.com.marealta.pousada.domain.RatePlan;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Valida o corpo do POST /reservations; qualquer defeito vira 400 INVALID_REQUEST. */
final class BookingParser {

    private BookingParser() {
    }

    static BookingRequest parse(String body) {
        JsonNode json;
        try {
            json = Json.MAPPER.readTree(body);
        } catch (Exception e) {
            throw invalid("O corpo da requisição não é um JSON válido");
        }
        if (json == null || !json.isObject()) {
            throw invalid("O corpo da requisição deve ser um objeto JSON");
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
        return new BookingRequest(integer(json, "roomNumber"), text(json, "guestName"), text(json, "guestEmail"),
                text(json, "guestCpf"), checkIn, checkOut, guests, ratePlan(json));
    }

    private static String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asText();
    }

    private static int integer(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asInt();
    }

    private static LocalDate date(JsonNode json, String field) {
        try {
            return LocalDate.parse(text(json, field));
        } catch (DateTimeParseException e) {
            throw invalid("Data inválida em " + field + "; use o formato 2027-07-10");
        }
    }

    private static RatePlan ratePlan(JsonNode json) {
        try {
            return RatePlan.valueOf(text(json, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw invalid("Plano de tarifa inválido; use STANDARD ou NON_REFUNDABLE");
        }
    }

    private static PousadaException invalid(String message) {
        return new PousadaException(ErrorCode.INVALID_REQUEST, message);
    }
}
