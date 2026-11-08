package br.com.mare.web;

import br.com.mare.domain.ApiException;
import br.com.mare.domain.ReservationRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/** Lê e valida o formato dos dados que chegam do site. */
final class RequestParser {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RequestParser() {}

    static ReservationRequest reservation(String body) {
        JsonNode json;
        try {
            json = MAPPER.readTree(body);
        } catch (Exception e) {
            throw ApiException.invalid("O corpo não é um JSON válido");
        }
        if (json == null || !json.isObject()) throw ApiException.invalid("O corpo deve ser um objeto JSON");

        JsonNode roomNode = json.get("roomNumber");
        JsonNode guestsNode = json.get("guests");
        if (roomNode == null || !roomNode.isInt()) throw ApiException.invalid("roomNumber é obrigatório e deve ser inteiro");
        if (guestsNode == null || !guestsNode.isInt()) throw ApiException.invalid("guests é obrigatório e deve ser inteiro");

        String name = text(json, "guestName");
        String email = text(json, "guestEmail");
        String cpf = text(json, "guestCpf");
        LocalDate checkIn = date(text(json, "checkIn"), "checkIn");
        LocalDate checkOut = date(text(json, "checkOut"), "checkOut");
        String plan = text(json, "ratePlan");

        if (!checkOut.isAfter(checkIn)) throw ApiException.invalid("A saída deve ser depois da entrada");
        if (guestsNode.asInt() < 1) throw ApiException.invalid("É preciso pelo menos um hóspede");

        JsonNode company = json.get("companyCode");
        String companyCode = company != null && company.isTextual() && !company.asText().isBlank()
                ? company.asText().trim() : null;

        return new ReservationRequest(roomNode.asInt(), name, email, cpf, checkIn, checkOut, guestsNode.asInt(),
                plan, companyCode);
    }

    static LocalDate date(String value, String field) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid(field + " deve estar no formato AAAA-MM-DD");
        }
    }

    static YearMonth month(String value) {
        if (value == null || !value.matches("\\d{4}-\\d{2}")) throw ApiException.invalid("month deve estar no formato AAAA-MM");
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid("month deve estar no formato AAAA-MM");
        }
    }

    private static String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw ApiException.invalid(field + " é obrigatório");
        }
        return node.asText();
    }
}
