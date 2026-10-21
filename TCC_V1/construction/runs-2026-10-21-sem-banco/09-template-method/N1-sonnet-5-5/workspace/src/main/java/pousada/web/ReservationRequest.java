package pousada.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import pousada.domain.RatePlan;
import pousada.error.BusinessException;
import pousada.error.ErrorCode;
import pousada.service.NewReservation;

final class ReservationRequest {
    private ReservationRequest() {
    }

    static NewReservation parse(ObjectMapper mapper, String body) {
        JsonNode json;
        try {
            json = mapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw invalid("O corpo não é um JSON válido");
        }
        if (json == null || !json.isObject()) {
            throw invalid("O corpo deve ser um objeto JSON");
        }
        LocalDate checkIn = date(json, "checkIn");
        LocalDate checkOut = date(json, "checkOut");
        int guests = integer(json, "guests");
        if (!checkOut.isAfter(checkIn)) {
            throw invalid("A saída deve ser depois da entrada");
        }
        if (guests < 1) {
            throw invalid("É preciso pelo menos um hóspede");
        }
        return new NewReservation(integer(json, "roomNumber"), text(json, "guestName"), text(json, "guestEmail"),
                text(json, "guestCpf"), checkIn, checkOut, guests, ratePlan(json));
    }

    private static String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asText();
    }

    private static int integer(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isInt()) {
            throw invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asInt();
    }

    private static LocalDate date(JsonNode json, String field) {
        try {
            return LocalDate.parse(text(json, field));
        } catch (DateTimeParseException e) {
            throw invalid("Data inválida em " + field + ", use o formato 2027-07-10");
        }
    }

    private static RatePlan ratePlan(JsonNode json) {
        try {
            return RatePlan.valueOf(text(json, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw invalid("Plano de tarifa inválido");
        }
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.INVALID_REQUEST, message);
    }
}
