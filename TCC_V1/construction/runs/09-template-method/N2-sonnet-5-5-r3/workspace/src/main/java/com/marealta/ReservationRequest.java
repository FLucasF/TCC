package com.marealta;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public record ReservationRequest(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                 LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static ReservationRequest parse(String body) {
        JsonNode json;
        try {
            json = MAPPER.readTree(body);
        } catch (JsonProcessingException e) {
            throw ApiException.invalidRequest("O corpo não é um JSON válido");
        }
        if (json == null || !json.isObject()) {
            throw ApiException.invalidRequest("O corpo deve ser um objeto JSON");
        }
        LocalDate checkIn = date(json, "checkIn");
        LocalDate checkOut = date(json, "checkOut");
        if (!checkOut.isAfter(checkIn)) {
            throw ApiException.invalidRequest("A saída deve ser depois da entrada");
        }
        int guests = integer(json, "guests");
        if (guests < 1) {
            throw ApiException.invalidRequest("É preciso ao menos um hóspede");
        }
        return new ReservationRequest(integer(json, "roomNumber"), text(json, "guestName"),
                text(json, "guestEmail"), text(json, "guestCpf"), checkIn, checkOut, guests, ratePlan(json));
    }

    private static String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw ApiException.invalidRequest("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asText();
    }

    private static int integer(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw ApiException.invalidRequest("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asInt();
    }

    private static LocalDate date(JsonNode json, String field) {
        try {
            return LocalDate.parse(text(json, field));
        } catch (DateTimeParseException e) {
            throw ApiException.invalidRequest("Data fora do formato AAAA-MM-DD: " + field);
        }
    }

    private static RatePlan ratePlan(JsonNode json) {
        try {
            return RatePlan.valueOf(text(json, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw ApiException.invalidRequest("Plano de tarifa desconhecido");
        }
    }
}
