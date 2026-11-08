package br.com.mare.web;

import br.com.mare.domain.BusinessException;
import br.com.mare.domain.ErrorCode;
import br.com.mare.domain.RatePlan;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Valida o corpo do POST /reservations. */
final class RequestParser {
    record NewReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                          LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
    }

    private final JsonNode json;

    private RequestParser(JsonNode json) {
        this.json = json;
    }

    static NewReservation parse(ObjectMapper mapper, String body) {
        JsonNode root;
        try {
            root = mapper.readTree(body);
        } catch (Exception e) {
            throw invalid("O corpo deve ser um JSON válido");
        }
        if (root == null || !root.isObject()) {
            throw invalid("O corpo deve ser um objeto JSON");
        }
        RequestParser p = new RequestParser(root);
        NewReservation r = new NewReservation(
                p.integer("roomNumber"), p.text("guestName"), p.text("guestEmail"), p.text("guestCpf"),
                p.date("checkIn"), p.date("checkOut"), p.integer("guests"), p.ratePlan());
        if (!r.checkOut().isAfter(r.checkIn())) {
            throw invalid("checkOut deve ser depois de checkIn");
        }
        if (r.guests() < 1) {
            throw invalid("guests deve ser pelo menos 1");
        }
        return r;
    }

    private String text(String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asText();
    }

    private int integer(String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asInt();
    }

    private LocalDate date(String field) {
        try {
            return LocalDate.parse(text(field));
        } catch (DateTimeParseException e) {
            throw invalid(field + " deve estar no formato 2027-07-10");
        }
    }

    private RatePlan ratePlan() {
        try {
            return RatePlan.valueOf(text("ratePlan"));
        } catch (IllegalArgumentException e) {
            throw invalid("ratePlan deve ser STANDARD ou NON_REFUNDABLE");
        }
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.INVALID_REQUEST, message);
    }
}
