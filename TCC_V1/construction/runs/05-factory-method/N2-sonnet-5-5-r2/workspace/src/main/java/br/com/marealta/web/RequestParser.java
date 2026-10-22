package br.com.marealta.web;

import br.com.marealta.ApiException;
import br.com.marealta.NewReservation;
import br.com.marealta.domain.Identity;
import br.com.marealta.domain.IdentityKinds;
import br.com.marealta.domain.RatePlan;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

final class RequestParser {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RequestParser() {
    }

    static NewReservation newReservation(String body) {
        JsonNode json;
        try {
            json = MAPPER.readTree(body);
        } catch (JsonProcessingException e) {
            throw ApiException.invalidRequest("Corpo não é um JSON válido");
        }
        if (json == null || !json.isObject()) {
            throw ApiException.invalidRequest("Corpo deve ser um objeto JSON");
        }
        int roomNumber = integer(json, "roomNumber");
        String name = text(json, "guestName");
        String email = text(json, "guestEmail");
        LocalDate checkIn = date(text(json, "checkIn"), "checkIn");
        LocalDate checkOut = date(text(json, "checkOut"), "checkOut");
        int guests = integer(json, "guests");
        RatePlan plan = ratePlan(text(json, "ratePlan"));
        if (!checkOut.isAfter(checkIn)) {
            throw ApiException.invalidRequest("A saída deve ser depois da entrada");
        }
        if (guests < 1) {
            throw ApiException.invalidRequest("É preciso pelo menos um hóspede");
        }
        Identity identity = IdentityKinds.from(identityFields(json)).orElseThrow(() -> ApiException.invalidRequest(
                "Informe guestCpf, ou companyCnpj com companyName, ou guestPassport com guestCountry"));
        return new NewReservation(roomNumber, name, email, identity, checkIn, checkOut, guests, plan);
    }

    static LocalDate day(String value) {
        if (value == null) {
            throw ApiException.invalidRequest("Parâmetro date é obrigatório");
        }
        return date(value, "date");
    }

    static YearMonth month(String value) {
        if (value == null) {
            throw ApiException.invalidRequest("Parâmetro month é obrigatório");
        }
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw ApiException.invalidRequest("month deve estar no formato 2027-07");
        }
    }

    private static Map<String, String> identityFields(JsonNode json) {
        Map<String, String> fields = new HashMap<>();
        for (String f : IdentityKinds.allFields()) {
            JsonNode node = json.get(f);
            if (node == null || node.isNull()) {
                continue;
            }
            if (!node.isTextual()) {
                throw ApiException.invalidRequest("Campo " + f + " deve ser texto");
            }
            fields.put(f, node.asText());
        }
        return fields;
    }

    private static String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw ApiException.invalidRequest("Campo " + field + " é obrigatório");
        }
        return node.asText();
    }

    private static int integer(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw ApiException.invalidRequest("Campo " + field + " deve ser um número inteiro");
        }
        return node.asInt();
    }

    private static LocalDate date(String value, String field) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw ApiException.invalidRequest("Campo " + field + " deve estar no formato 2027-07-10");
        }
    }

    private static RatePlan ratePlan(String value) {
        try {
            return RatePlan.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw ApiException.invalidRequest("ratePlan deve ser STANDARD ou NON_REFUNDABLE");
        }
    }
}
