package br.com.marealta.web;

import br.com.marealta.domain.AppException;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.identity.GuestIdentity;
import br.com.marealta.identity.IdentityKind;
import br.com.marealta.identity.IdentityKinds;
import br.com.marealta.service.ReservationService.NewReservation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

final class ReservationRequestParser {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ReservationRequestParser() {}

    static NewReservation parse(String body) {
        JsonNode json = readObject(body);
        LocalDate checkIn = date(json, "checkIn");
        LocalDate checkOut = date(json, "checkOut");
        if (!checkOut.isAfter(checkIn)) {
            throw AppException.invalid("A saída precisa ser depois da entrada");
        }
        JsonNode guests = json.get("guests");
        if (guests == null || !guests.isInt() || guests.intValue() < 1) {
            throw AppException.invalid("guests precisa ser um número inteiro maior que zero");
        }
        return new NewReservation(roomNumber(json), text(json, "guestName"), text(json, "guestEmail"),
                identity(json), checkIn, checkOut, guests.intValue(), ratePlan(json));
    }

    private static JsonNode readObject(String body) {
        try {
            JsonNode json = MAPPER.readTree(body);
            if (json == null || !json.isObject()) {
                throw AppException.invalid("O corpo precisa ser um objeto JSON");
            }
            return json;
        } catch (JsonProcessingException e) {
            throw AppException.invalid("O corpo não é um JSON válido");
        }
    }

    private static int roomNumber(JsonNode json) {
        JsonNode node = json.get("roomNumber");
        if (node == null || !node.isInt()) {
            throw AppException.invalid("roomNumber precisa ser um número inteiro");
        }
        return node.intValue();
    }

    private static RatePlan ratePlan(JsonNode json) {
        try {
            return RatePlan.valueOf(text(json, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw AppException.invalid("ratePlan desconhecido");
        }
    }

    private static LocalDate date(JsonNode json, String field) {
        try {
            return LocalDate.parse(text(json, field));
        } catch (DateTimeParseException e) {
            throw AppException.invalid(field + " precisa estar no formato AAAA-MM-DD");
        }
    }

    private static GuestIdentity identity(JsonNode json) {
        Map<String, String> given = new LinkedHashMap<>();
        for (String field : IdentityKinds.allFieldNames()) {
            if (json.hasNonNull(field)) {
                given.put(field, text(json, field));
            }
        }
        IdentityKind kind = IdentityKinds.resolve(given.keySet()).orElseThrow(() -> AppException.invalid(
                "Informe a identificação de um único jeito: " + IdentityKinds.allFieldNames()));
        return new GuestIdentity(kind, given);
    }

    private static String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.textValue().isBlank()) {
            throw AppException.invalid(field + " é obrigatório e precisa ser um texto");
        }
        return node.textValue();
    }
}
