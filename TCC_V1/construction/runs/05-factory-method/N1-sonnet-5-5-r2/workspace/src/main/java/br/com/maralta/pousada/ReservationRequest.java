package br.com.maralta.pousada;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Corpo validado do POST /reservations. */
public record ReservationRequest(
        int roomNumber,
        String guestName,
        String guestEmail,
        Identification identification,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan) {

    public static ReservationRequest parse(String body) {
        JsonNode json;
        try {
            json = Json.MAPPER.readTree(body);
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
            throw ApiException.invalidRequest("É preciso pelo menos um hóspede");
        }
        return new ReservationRequest(integer(json, "roomNumber"), text(json, "guestName"),
                text(json, "guestEmail"), identification(json), checkIn, checkOut, guests, ratePlan(json));
    }

    private static Identification identification(JsonNode json) {
        Map<String, String> given = new LinkedHashMap<>();
        for (String name : Identifications.allFieldNames()) {
            JsonNode value = json.get(name);
            if (value == null || value.isNull()) {
                continue;
            }
            if (!value.isTextual()) {
                throw ApiException.invalidRequest("Campo " + name + " deve ser um texto");
            }
            given.put(name, value.asText());
        }
        return Identifications.from(given);
    }

    private static RatePlan ratePlan(JsonNode json) {
        try {
            return RatePlan.valueOf(text(json, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw ApiException.invalidRequest("ratePlan inválido");
        }
    }

    private static String text(JsonNode json, String field) {
        JsonNode value = json.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw ApiException.invalidRequest("Campo obrigatório: " + field);
        }
        return value.asText();
    }

    private static int integer(JsonNode json, String field) {
        JsonNode value = json.get(field);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToInt()) {
            throw ApiException.invalidRequest("Campo obrigatório (número inteiro): " + field);
        }
        return value.asInt();
    }

    private static LocalDate date(JsonNode json, String field) {
        try {
            return LocalDate.parse(text(json, field));
        } catch (DateTimeParseException e) {
            throw ApiException.invalidRequest("Data inválida em " + field + " (use 2027-07-10)");
        }
    }
}
