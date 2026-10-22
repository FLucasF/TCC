package br.com.marealta.web;

import br.com.marealta.domain.ApiException;
import br.com.marealta.plan.RatePlans;
import br.com.marealta.service.NewReservation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/** Confere o formato do que chega do site; as regras de negócio ficam com o serviço. */
final class RequestParser {

    private RequestParser() {
    }

    static NewReservation reservation(ObjectMapper mapper, String body) {
        JsonNode json = readObject(mapper, body);
        LocalDate checkIn = date(json, "checkIn");
        LocalDate checkOut = date(json, "checkOut");
        if (!checkOut.isAfter(checkIn)) {
            throw ApiException.invalidRequest("A saída deve ser depois da entrada");
        }
        int guests = guests(json);
        String ratePlan = text(json, "ratePlan");
        if (RatePlans.find(ratePlan).isEmpty()) {
            throw ApiException.invalidRequest("Plano de tarifa desconhecido: " + ratePlan);
        }
        JsonNode companyCode = json.get("companyCode");
        return new NewReservation(
                roomNumber(json),
                text(json, "guestName"),
                text(json, "guestEmail"),
                text(json, "guestCpf"),
                checkIn,
                checkOut,
                guests,
                ratePlan,
                companyCode != null && companyCode.isTextual() ? companyCode.asText() : null);
    }

    static LocalDate date(String value, String field) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw ApiException.invalidRequest("Data inválida em " + field + ", use o formato 2027-07-10");
        }
    }

    static YearMonth month(String value) {
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw ApiException.invalidRequest("Mês inválido, use o formato 2027-07");
        }
    }

    private static JsonNode readObject(ObjectMapper mapper, String body) {
        try {
            JsonNode json = mapper.readTree(body);
            if (json == null || !json.isObject()) {
                throw ApiException.invalidRequest("O corpo deve ser um objeto JSON");
            }
            return json;
        } catch (JsonProcessingException e) {
            throw ApiException.invalidRequest("O corpo não é um JSON válido");
        }
    }

    private static String text(JsonNode json, String field) {
        JsonNode value = json.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw ApiException.invalidRequest("Campo obrigatório ausente ou inválido: " + field);
        }
        return value.asText();
    }

    private static LocalDate date(JsonNode json, String field) {
        return date(text(json, field), field);
    }

    private static int roomNumber(JsonNode json) {
        JsonNode value = json.get("roomNumber");
        if (value == null || !value.isIntegralNumber() || !value.canConvertToInt()) {
            throw ApiException.invalidRequest("Campo obrigatório ausente ou inválido: roomNumber");
        }
        return value.asInt();
    }

    private static int guests(JsonNode json) {
        JsonNode value = json.get("guests");
        if (value == null || !value.isIntegralNumber() || !value.canConvertToInt()) {
            throw ApiException.invalidRequest("Campo obrigatório ausente ou inválido: guests");
        }
        if (value.asInt() < 1) {
            throw ApiException.invalidRequest("É preciso ao menos 1 hóspede");
        }
        return value.asInt();
    }
}
