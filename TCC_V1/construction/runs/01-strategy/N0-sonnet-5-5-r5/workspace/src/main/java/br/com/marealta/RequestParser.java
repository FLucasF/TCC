package br.com.marealta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public final class RequestParser {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RequestParser() {}

    public static ReservationService.NewReservation parseNewReservation(String body) {
        JsonNode n;
        try {
            n = MAPPER.readTree(body == null ? "" : body);
        } catch (Exception e) {
            throw ApiException.invalid("O corpo não é um JSON válido.");
        }
        if (n == null || !n.isObject()) {
            throw ApiException.invalid("O corpo deve ser um objeto JSON.");
        }
        int room = integer(n, "roomNumber");
        String name = text(n, "guestName");
        String email = text(n, "guestEmail");
        String cpf = text(n, "guestCpf");
        LocalDate in = date(n, "checkIn");
        LocalDate out = date(n, "checkOut");
        int guests = integer(n, "guests");
        String plan = text(n, "ratePlan");
        if (!out.isAfter(in)) {
            throw ApiException.invalid("A saída deve ser depois da entrada.");
        }
        if (guests < 1) {
            throw ApiException.invalid("É preciso pelo menos um hóspede.");
        }
        JsonNode cc = n.get("companyCode");
        String company = cc != null && cc.isTextual() ? cc.asText().trim() : null;
        return new ReservationService.NewReservation(room, name, email, cpf, in, out, guests, plan, company);
    }

    public static LocalDate parseDate(String s) {
        try {
            return LocalDate.parse(s == null ? "" : s);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid("Data inválida; use o formato AAAA-MM-DD.");
        }
    }

    public static YearMonth parseMonth(String s) {
        try {
            return YearMonth.parse(s == null ? "" : s);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid("Mês inválido; use o formato AAAA-MM.");
        }
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v == null || !v.isTextual() || v.asText().isBlank()) {
            throw ApiException.invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return v.asText().trim();
    }

    private static int integer(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v == null || !v.isIntegralNumber() || !v.canConvertToInt()) {
            throw ApiException.invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return v.asInt();
    }

    private static LocalDate date(JsonNode n, String field) {
        return parseDate(text(n, field));
    }
}
