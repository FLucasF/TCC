package pousada.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pousada.BusinessException;
import pousada.ErrorCode;
import pousada.domain.RatePlan;
import pousada.domain.NewReservation;
import pousada.identity.GuestIdentity;
import pousada.identity.IdentityKind;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

/** Lê e valida o corpo do POST /reservations. */
final class ReservationRequest {
    private static final ObjectMapper JSON = new ObjectMapper();

    private ReservationRequest() {
    }

    static NewReservation parse(String body) {
        JsonNode json = readObject(body);
        int roomNumber = integer(json, "roomNumber");
        String guestName = text(json, "guestName");
        String guestEmail = text(json, "guestEmail");
        GuestIdentity identity = IdentityKind.fromValues(identityValues(json));
        LocalDate checkIn = date(json, "checkIn");
        LocalDate checkOut = date(json, "checkOut");
        int guests = integer(json, "guests");
        RatePlan ratePlan = ratePlan(json);

        if (!checkOut.isAfter(checkIn)) {
            throw invalid("checkOut deve ser depois de checkIn");
        }
        if (guests < 1) {
            throw invalid("guests deve ser pelo menos 1");
        }
        return new NewReservation(roomNumber, guestName, guestEmail, identity, checkIn, checkOut, guests, ratePlan);
    }

    private static JsonNode readObject(String body) {
        try {
            JsonNode json = JSON.readTree(body);
            if (json == null || !json.isObject()) {
                throw invalid("O corpo deve ser um objeto JSON");
            }
            return json;
        } catch (JsonProcessingException e) {
            throw invalid("O corpo não é um JSON válido");
        }
    }

    private static Map<String, String> identityValues(JsonNode json) {
        Map<String, String> values = new HashMap<>();
        for (String name : IdentityKind.allFieldNames()) {
            JsonNode node = json.get(name);
            if (node == null || node.isNull()) {
                continue;
            }
            if (!node.isTextual()) {
                throw invalid(name + " deve ser texto");
            }
            values.put(name, node.asText());
        }
        return values;
    }

    private static String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw invalid("Campo obrigatório (texto): " + field);
        }
        return node.asText();
    }

    private static int integer(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw invalid("Campo obrigatório (número inteiro): " + field);
        }
        return node.asInt();
    }

    private static LocalDate date(JsonNode json, String field) {
        String value = text(json, field);
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw invalid(field + " deve estar no formato AAAA-MM-DD");
        }
    }

    private static RatePlan ratePlan(JsonNode json) {
        String value = text(json, "ratePlan");
        try {
            return RatePlan.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw invalid("ratePlan deve ser STANDARD ou NON_REFUNDABLE");
        }
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.INVALID_REQUEST, message);
    }
}
