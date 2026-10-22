package pousada.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pousada.domain.PousadaException;
import pousada.domain.RatePlan;
import pousada.identity.IdentityKinds;
import pousada.service.NewReservation;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

final class RequestParser {
    private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern MONTH = Pattern.compile("\\d{4}-\\d{2}");

    private final ObjectMapper mapper;

    RequestParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    NewReservation newReservation(String body) {
        JsonNode json = parseObject(body);
        LocalDate checkIn = date(text(json, "checkIn"), "checkIn");
        LocalDate checkOut = date(text(json, "checkOut"), "checkOut");
        if (!checkOut.isAfter(checkIn)) {
            throw PousadaException.invalid("A saída deve ser depois da entrada");
        }
        int guests = integer(json, "guests");
        if (guests < 1) {
            throw PousadaException.invalid("É preciso pelo menos um hóspede");
        }
        return new NewReservation(
                integer(json, "roomNumber"),
                text(json, "guestName"),
                text(json, "guestEmail"),
                IdentityKinds.fromRequest(identityFields(json)),
                checkIn,
                checkOut,
                guests,
                ratePlan(text(json, "ratePlan")));
    }

    static LocalDate date(String value, String name) {
        if (value == null || !DATE.matcher(value).matches()) {
            throw PousadaException.invalid("Data inválida em " + name + " (use AAAA-MM-DD)");
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw PousadaException.invalid("Data inválida em " + name + " (use AAAA-MM-DD)");
        }
    }

    static YearMonth month(String value) {
        if (value == null || !MONTH.matcher(value).matches()) {
            throw PousadaException.invalid("Mês inválido (use AAAA-MM)");
        }
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw PousadaException.invalid("Mês inválido (use AAAA-MM)");
        }
    }

    private JsonNode parseObject(String body) {
        try {
            JsonNode json = mapper.readTree(body);
            if (json == null || !json.isObject()) {
                throw PousadaException.invalid("O corpo deve ser um objeto JSON");
            }
            return json;
        } catch (JsonProcessingException e) {
            throw PousadaException.invalid("O corpo não é um JSON válido");
        }
    }

    private static Map<String, String> identityFields(JsonNode json) {
        Map<String, String> given = new HashMap<>();
        for (String name : IdentityKinds.allFieldNames()) {
            if (json.hasNonNull(name)) {
                given.put(name, text(json, name));
            }
        }
        return given;
    }

    private static String text(JsonNode json, String name) {
        JsonNode node = json.get(name);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw PousadaException.invalid("Campo obrigatório: " + name);
        }
        return node.asText();
    }

    private static int integer(JsonNode json, String name) {
        JsonNode node = json.get(name);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw PousadaException.invalid("Campo obrigatório (número inteiro): " + name);
        }
        return node.asInt();
    }

    private static RatePlan ratePlan(String value) {
        try {
            return RatePlan.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw PousadaException.invalid("Plano de tarifa inválido: " + value);
        }
    }
}
