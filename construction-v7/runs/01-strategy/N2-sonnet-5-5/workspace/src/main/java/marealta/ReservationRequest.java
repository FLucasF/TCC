package marealta;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Lê e valida o corpo do POST /reservations. */
public final class ReservationRequest {
    private ReservationRequest() {}

    public static ReservationService.NewReservation parse(String body, ObjectMapper mapper, RatePlans plans) {
        JsonNode json;
        try {
            json = mapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw ApiException.invalid("O corpo não é um JSON válido");
        }
        if (json == null || !json.isObject()) {
            throw ApiException.invalid("O corpo deve ser um objeto JSON");
        }
        LocalDate checkIn = date(json, "checkIn");
        LocalDate checkOut = date(json, "checkOut");
        if (!checkOut.isAfter(checkIn)) {
            throw ApiException.invalid("A saída deve ser depois da entrada");
        }
        JsonNode guests = json.get("guests");
        JsonNode room = json.get("roomNumber");
        if (guests == null || !guests.isIntegralNumber() || !guests.canConvertToInt()) {
            throw ApiException.invalid("Campo guests ausente ou inválido");
        }
        if (room == null || !room.isIntegralNumber() || !room.canConvertToInt()) {
            throw ApiException.invalid("Campo roomNumber ausente ou inválido");
        }
        if (guests.asInt() < 1) {
            throw ApiException.invalid("É preciso pelo menos um hóspede");
        }
        String planCode = text(json, "ratePlan");
        RatePlan plan = plans.find(planCode)
                .orElseThrow(() -> ApiException.invalid("Plano de tarifa desconhecido: " + planCode));
        JsonNode company = json.get("companyCode");
        String companyCode = company != null && company.isTextual() ? company.asText() : null;
        return new ReservationService.NewReservation(room.asInt(), text(json, "guestName"),
                text(json, "guestEmail"), text(json, "guestCpf"), checkIn, checkOut, guests.asInt(),
                plan, companyCode);
    }

    private static String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw ApiException.invalid("Campo " + field + " ausente ou inválido");
        }
        return node.asText();
    }

    private static LocalDate date(JsonNode json, String field) {
        try {
            return LocalDate.parse(text(json, field));
        } catch (DateTimeParseException e) {
            throw ApiException.invalid("Campo " + field + " deve estar no formato AAAA-MM-DD");
        }
    }
}
