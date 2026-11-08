package br.com.marealta.web;

import br.com.marealta.domain.DomainException;
import br.com.marealta.domain.NewReservation;
import br.com.marealta.domain.RatePlan;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

final class RequestParser {
    private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private RequestParser() {}

    static NewReservation reservation(ObjectMapper mapper, String body) {
        JsonNode json;
        try {
            json = mapper.readTree(body);
        } catch (Exception e) {
            throw DomainException.invalid("O corpo não é um JSON válido");
        }
        if (json == null || !json.isObject()) {
            throw DomainException.invalid("O corpo deve ser um objeto JSON");
        }
        int roomNumber = integer(json, "roomNumber");
        String name = text(json, "guestName");
        String email = text(json, "guestEmail");
        String cpf = text(json, "guestCpf");
        LocalDate checkIn = date(json, "checkIn");
        LocalDate checkOut = date(json, "checkOut");
        int guests = integer(json, "guests");
        RatePlan plan;
        try {
            plan = RatePlan.valueOf(text(json, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw DomainException.invalid("ratePlan deve ser STANDARD ou NON_REFUNDABLE");
        }
        return new NewReservation(roomNumber, name, email, cpf, checkIn, checkOut, guests, plan);
    }

    private static String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw DomainException.invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asText().trim();
    }

    private static int integer(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw DomainException.invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asInt();
    }

    private static LocalDate date(JsonNode json, String field) {
        String raw = text(json, field);
        if (!DATE.matcher(raw).matches()) {
            throw DomainException.invalid("Data inválida em " + field + " (use AAAA-MM-DD)");
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw DomainException.invalid("Data inválida em " + field);
        }
    }
}
