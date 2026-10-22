package br.com.mealta.web;

import br.com.mealta.domain.CreateReservation;
import br.com.mealta.domain.ErrorCode;
import br.com.mealta.domain.PousadaException;
import br.com.mealta.domain.RatePlan;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Transforma o corpo JSON do POST /reservations em um pedido, recusando o que estiver faltando ou torto. */
public class CreateReservationParser {
    private final ObjectMapper mapper;

    public CreateReservationParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public CreateReservation parse(String body) {
        JsonNode json;
        try {
            json = mapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw invalid("O corpo não é um JSON válido");
        }
        if (json == null || !json.isObject()) {
            throw invalid("O corpo deve ser um objeto JSON");
        }
        return new CreateReservation(
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
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw invalid("Campo '" + field + "' é obrigatório");
        }
        return node.asText();
    }

    private static int integer(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw invalid("Campo '" + field + "' deve ser um número inteiro");
        }
        return node.asInt();
    }

    private static LocalDate date(JsonNode json, String field) {
        try {
            return LocalDate.parse(text(json, field));
        } catch (DateTimeParseException e) {
            throw invalid("Campo '" + field + "' deve ser uma data AAAA-MM-DD");
        }
    }

    private static RatePlan ratePlan(JsonNode json) {
        try {
            return RatePlan.valueOf(text(json, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw invalid("Campo 'ratePlan' deve ser STANDARD ou NON_REFUNDABLE");
        }
    }

    private static PousadaException invalid(String message) {
        return new PousadaException(ErrorCode.INVALID_REQUEST, message);
    }
}
