package br.com.marealta;

import br.com.marealta.plan.RatePlan;
import br.com.marealta.plan.RatePlans;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Pedido de reserva já validado quanto ao formato. */
public record NewReservation(
        int roomNumber,
        String guestName,
        String guestEmail,
        String guestCpf,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan plan,
        String companyCode) {

    public static NewReservation parse(ObjectMapper mapper, String body) {
        JsonNode json;
        try {
            json = mapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw ApiError.invalidRequest("O corpo não é um JSON válido");
        }
        if (json == null || !json.isObject()) {
            throw ApiError.invalidRequest("O corpo deve ser um objeto JSON");
        }
        LocalDate checkIn = date(json, "checkIn");
        LocalDate checkOut = date(json, "checkOut");
        if (!checkOut.isAfter(checkIn)) {
            throw ApiError.invalidRequest("A saída deve ser depois da entrada");
        }
        int guests = integer(json, "guests");
        if (guests < 1) {
            throw ApiError.invalidRequest("É preciso ao menos um hóspede");
        }
        String planCode = text(json, "ratePlan");
        RatePlan plan = RatePlans.find(planCode)
                .orElseThrow(() -> ApiError.invalidRequest("Plano de tarifa desconhecido: " + planCode));
        JsonNode company = json.get("companyCode");
        return new NewReservation(
                integer(json, "roomNumber"),
                text(json, "guestName"),
                text(json, "guestEmail"),
                text(json, "guestCpf"),
                checkIn,
                checkOut,
                guests,
                plan,
                company != null && company.isTextual() ? company.asText() : null);
    }

    private static String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw ApiError.invalidRequest("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asText();
    }

    private static int integer(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw ApiError.invalidRequest("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asInt();
    }

    private static LocalDate date(JsonNode json, String field) {
        try {
            return LocalDate.parse(text(json, field));
        } catch (DateTimeParseException e) {
            throw ApiError.invalidRequest("Data fora do formato AAAA-MM-DD: " + field);
        }
    }
}
