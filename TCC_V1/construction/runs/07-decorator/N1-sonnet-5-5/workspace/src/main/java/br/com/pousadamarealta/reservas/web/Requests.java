package br.com.pousadamarealta.reservas.web;

import br.com.pousadamarealta.reservas.domain.DomainException;
import br.com.pousadamarealta.reservas.domain.ErrorCode;
import br.com.pousadamarealta.reservas.domain.RatePlan;
import br.com.pousadamarealta.reservas.service.NewReservation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/** Leitura e validação do formato do que chega pelo HTTP. */
final class Requests {

    private Requests() {
    }

    static NewReservation newReservation(ObjectMapper mapper, String json) {
        JsonNode body;
        try {
            body = mapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw invalid("o corpo não é um JSON válido");
        }
        if (body == null || !body.isObject()) {
            throw invalid("o corpo deve ser um objeto JSON");
        }
        return new NewReservation(
                integer(body, "roomNumber"),
                text(body, "guestName"),
                text(body, "guestEmail"),
                text(body, "guestCpf"),
                date(text(body, "checkIn"), "checkIn"),
                date(text(body, "checkOut"), "checkOut"),
                integer(body, "guests"),
                ratePlan(text(body, "ratePlan")));
    }

    static LocalDate date(String value, String field) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw invalid(field + " deve estar no formato AAAA-MM-DD");
        }
    }

    static YearMonth month(String value) {
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw invalid("month deve estar no formato AAAA-MM");
        }
    }

    private static String text(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw invalid(field + " é obrigatório");
        }
        return node.asText();
    }

    private static int integer(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || !node.isInt()) {
            throw invalid(field + " é obrigatório e deve ser um número inteiro");
        }
        return node.asInt();
    }

    private static RatePlan ratePlan(String value) {
        try {
            return RatePlan.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw invalid("ratePlan deve ser STANDARD ou NON_REFUNDABLE");
        }
    }

    private static DomainException invalid(String message) {
        return new DomainException(ErrorCode.INVALID_REQUEST, message);
    }
}
