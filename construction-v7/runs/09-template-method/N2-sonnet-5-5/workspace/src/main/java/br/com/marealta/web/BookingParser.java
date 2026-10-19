package br.com.marealta.web;

import br.com.marealta.domain.BusinessException;
import br.com.marealta.domain.ErrorCode;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.service.BookingRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Valida o corpo do POST /reservations. */
final class BookingParser {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private BookingParser() {}

    static BookingRequest parse(String body) {
        JsonNode json;
        try {
            json = MAPPER.readTree(body);
        } catch (Exception e) {
            throw invalid("Corpo não é um JSON válido");
        }
        if (json == null || !json.isObject()) throw invalid("Corpo deve ser um objeto JSON");

        int roomNumber = integer(json, "roomNumber");
        int guests = integer(json, "guests");
        LocalDate checkIn = date(json, "checkIn");
        LocalDate checkOut = date(json, "checkOut");
        RatePlan plan;
        try {
            plan = RatePlan.valueOf(text(json, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw invalid("ratePlan inválido");
        }
        if (!checkOut.isAfter(checkIn)) throw invalid("A saída deve ser depois da entrada");
        if (guests < 1) throw invalid("Deve haver ao menos um hóspede");
        return new BookingRequest(roomNumber, text(json, "guestName"), text(json, "guestEmail"),
                text(json, "guestCpf"), checkIn, checkOut, guests, plan);
    }

    private static String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) throw invalid("Campo obrigatório: " + field);
        return node.asText();
    }

    private static int integer(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw invalid("Campo obrigatório (inteiro): " + field);
        }
        return node.asInt();
    }

    private static LocalDate date(JsonNode json, String field) {
        try {
            return LocalDate.parse(text(json, field));
        } catch (DateTimeParseException e) {
            throw invalid("Data inválida: " + field);
        }
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.INVALID_REQUEST, message);
    }
}
