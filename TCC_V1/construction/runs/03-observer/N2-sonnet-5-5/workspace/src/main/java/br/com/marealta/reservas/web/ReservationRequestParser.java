package br.com.marealta.reservas.web;

import br.com.marealta.reservas.domain.DomainException;
import br.com.marealta.reservas.domain.NewReservation;
import br.com.marealta.reservas.domain.RatePlan;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

final class ReservationRequestParser {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ReservationRequestParser() {}

    static NewReservation parse(String body) {
        JsonNode json;
        try {
            json = MAPPER.readTree(body);
        } catch (JsonProcessingException e) {
            throw DomainException.invalidRequest("O corpo precisa ser um JSON válido");
        }
        if (json == null || !json.isObject()) throw DomainException.invalidRequest("O corpo precisa ser um objeto JSON");

        LocalDate checkIn = date(json, "checkIn");
        LocalDate checkOut = date(json, "checkOut");
        if (!checkOut.isAfter(checkIn)) throw DomainException.invalidRequest("A saída precisa ser depois da entrada");
        int guests = integer(json, "guests");
        if (guests < 1) throw DomainException.invalidRequest("É preciso ao menos um hóspede");
        return new NewReservation(integer(json, "roomNumber"), text(json, "guestName"), text(json, "guestEmail"),
                text(json, "guestCpf"), checkIn, checkOut, guests, ratePlan(json));
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
            throw DomainException.invalidRequest("Data fora do formato AAAA-MM-DD: " + field);
        }
    }

    private static RatePlan ratePlan(JsonNode json) {
        try {
            return RatePlan.valueOf(text(json, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw DomainException.invalidRequest("Plano de tarifa inválido");
        }
    }
}
