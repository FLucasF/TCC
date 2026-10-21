package br.com.marealta.web;

import br.com.marealta.domain.DomainException;
import br.com.marealta.domain.ErrorCode;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.service.NewReservation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Lê o corpo do POST /reservations; qualquer defeito de formato vira INVALID_REQUEST. */
final class RequestBody {
    private final JsonNode json;

    private RequestBody(JsonNode json) {
        this.json = json;
    }

    static NewReservation parseNewReservation(ObjectMapper mapper, String body) {
        JsonNode json;
        try {
            json = mapper.readTree(body);
        } catch (Exception e) {
            throw invalid("O corpo não é um JSON válido");
        }
        if (json == null || !json.isObject()) {
            throw invalid("O corpo deve ser um objeto JSON");
        }
        RequestBody b = new RequestBody(json);
        return new NewReservation(b.integer("roomNumber"), b.text("guestName"), b.text("guestEmail"),
                b.text("guestCpf"), b.date("checkIn"), b.date("checkOut"), b.integer("guests"), b.ratePlan());
    }

    private String text(String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw invalid("Campo obrigatório ou inválido: " + field);
        }
        return node.asText();
    }

    private int integer(String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw invalid("Campo obrigatório ou inválido: " + field);
        }
        return node.asInt();
    }

    private LocalDate date(String field) {
        try {
            return LocalDate.parse(text(field));
        } catch (DateTimeParseException e) {
            throw invalid("Data inválida em " + field + " (use 2027-07-10)");
        }
    }

    private RatePlan ratePlan() {
        try {
            return RatePlan.valueOf(text("ratePlan"));
        } catch (IllegalArgumentException e) {
            throw invalid("Plano de tarifa inválido em ratePlan");
        }
    }

    private static DomainException invalid(String message) {
        return new DomainException(ErrorCode.INVALID_REQUEST, message);
    }
}
