package br.com.marealta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Corpo validado do POST /reservations. */
record ReservationRequest(int roomNumber, String guestName, String guestEmail, String guestCpf,
                          LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {

    static ReservationRequest parse(ObjectMapper mapper, String body) {
        JsonNode json;
        try {
            json = mapper.readTree(body);
        } catch (Exception e) {
            throw DomainException.invalidRequest("Corpo não é um JSON válido");
        }
        if (json == null || !json.isObject()) {
            throw DomainException.invalidRequest("Corpo deve ser um objeto JSON");
        }
        LocalDate checkIn = date(json, "checkIn");
        LocalDate checkOut = date(json, "checkOut");
        int guests = integer(json, "guests");
        if (!checkOut.isAfter(checkIn)) {
            throw DomainException.invalidRequest("A saída deve ser depois da entrada");
        }
        if (guests < 1) {
            throw DomainException.invalidRequest("É preciso pelo menos um hóspede");
        }
        RatePlan plan;
        try {
            plan = RatePlan.valueOf(text(json, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw DomainException.invalidRequest("ratePlan inválido");
        }
        return new ReservationRequest(integer(json, "roomNumber"), text(json, "guestName"),
                text(json, "guestEmail"), text(json, "guestCpf"), checkIn, checkOut, guests, plan);
    }

    private static String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw DomainException.invalidRequest("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asText();
    }

    private static int integer(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw DomainException.invalidRequest("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asInt();
    }

    private static LocalDate date(JsonNode json, String field) {
        try {
            return LocalDate.parse(text(json, field));
        } catch (DateTimeParseException e) {
            throw DomainException.invalidRequest("Data inválida em " + field);
        }
    }
}
