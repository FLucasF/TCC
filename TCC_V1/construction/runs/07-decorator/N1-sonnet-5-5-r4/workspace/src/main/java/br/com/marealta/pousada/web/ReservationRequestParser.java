package br.com.marealta.pousada.web;

import br.com.marealta.pousada.domain.DomainException;
import br.com.marealta.pousada.domain.ErrorCode;
import br.com.marealta.pousada.domain.NewReservation;
import br.com.marealta.pousada.domain.RatePlan;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

final class ReservationRequestParser {
    private final ObjectMapper mapper;

    ReservationRequestParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    NewReservation parse(String body) {
        JsonNode json;
        try {
            json = mapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw invalid("O corpo não é um JSON válido");
        }
        if (json == null || !json.isObject()) {
            throw invalid("O corpo deve ser um objeto JSON");
        }
        return new NewReservation(
                integer(json, "roomNumber"),
                text(json, "guestName"),
                text(json, "guestEmail"),
                text(json, "guestCpf"),
                date(json, "checkIn"),
                date(json, "checkOut"),
                integer(json, "guests"),
                ratePlan(json));
    }

    private static String text(JsonNode json, String field) {
        JsonNode value = json.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return value.asText();
    }

    private static int integer(JsonNode json, String field) {
        JsonNode value = json.get(field);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToInt()) {
            throw invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return value.asInt();
    }

    private static LocalDate date(JsonNode json, String field) {
        try {
            return LocalDate.parse(text(json, field));
        } catch (DateTimeParseException e) {
            throw invalid("Data inválida em " + field + " (use 2027-07-10)");
        }
    }

    private static RatePlan ratePlan(JsonNode json) {
        String value = text(json, "ratePlan");
        return Arrays.stream(RatePlan.values()).filter(plan -> plan.name().equals(value)).findFirst()
                .orElseThrow(() -> invalid("Plano de tarifa inválido: " + value));
    }

    private static DomainException invalid(String message) {
        return new DomainException(ErrorCode.INVALID_REQUEST, message);
    }
}
