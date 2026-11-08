package br.com.marealta.web;

import br.com.marealta.app.ReservationService.NewReservation;
import br.com.marealta.domain.ErrorCode;
import br.com.marealta.domain.PousadaException;
import br.com.marealta.domain.RatePlan;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Lê e valida o corpo do POST /reservations. */
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
        int roomNumber = integer(json, "roomNumber");
        String guestName = text(json, "guestName");
        String guestEmail = text(json, "guestEmail");
        String guestCpf = text(json, "guestCpf");
        LocalDate checkIn = date(json, "checkIn");
        LocalDate checkOut = date(json, "checkOut");
        int guests = integer(json, "guests");
        RatePlan ratePlan = ratePlan(json);
        if (!checkOut.isAfter(checkIn)) {
            throw invalid("A saída deve ser depois da entrada");
        }
        if (guests < 1) {
            throw invalid("É preciso ao menos um hóspede");
        }
        return new NewReservation(roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan);
    }

    private static String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw invalid("O campo " + field + " é obrigatório");
        }
        return node.asText().trim();
    }

    private static int integer(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw invalid("O campo " + field + " deve ser um número inteiro");
        }
        return node.intValue();
    }

    private static LocalDate date(JsonNode json, String field) {
        String value = text(json, field);
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw invalid("O campo " + field + " deve estar no formato AAAA-MM-DD");
        }
    }

    private static RatePlan ratePlan(JsonNode json) {
        String value = text(json, "ratePlan");
        try {
            return RatePlan.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw invalid("O campo ratePlan deve ser STANDARD ou NON_REFUNDABLE");
        }
    }

    private static PousadaException invalid(String message) {
        return new PousadaException(ErrorCode.INVALID_REQUEST, message);
    }
}
