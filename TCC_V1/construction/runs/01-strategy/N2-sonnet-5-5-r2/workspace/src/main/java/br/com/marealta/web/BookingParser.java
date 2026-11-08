package br.com.marealta.web;

import br.com.marealta.domain.BusinessException;
import br.com.marealta.plan.RatePlans;
import br.com.marealta.service.BookingRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Valida o corpo do POST /reservations e o transforma em um pedido de reserva. */
final class BookingParser {
    private final ObjectMapper mapper;

    BookingParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    BookingRequest parse(String body) {
        JsonNode json;
        try {
            json = mapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw BusinessException.invalid("O corpo não é um JSON válido");
        }
        if (json == null || !json.isObject()) {
            throw BusinessException.invalid("O corpo deve ser um objeto JSON");
        }
        int roomNumber = integer(json, "roomNumber");
        String name = text(json, "guestName");
        String email = text(json, "guestEmail");
        String cpf = text(json, "guestCpf");
        LocalDate checkIn = date(json, "checkIn");
        LocalDate checkOut = date(json, "checkOut");
        int guests = integer(json, "guests");
        String planCode = text(json, "ratePlan");
        if (!checkOut.isAfter(checkIn)) {
            throw BusinessException.invalid("A saída deve ser depois da entrada");
        }
        if (guests < 1) {
            throw BusinessException.invalid("É preciso pelo menos um hóspede");
        }
        JsonNode company = json.get("companyCode");
        String companyCode = company != null && company.isTextual() ? company.asText() : null;
        return new BookingRequest(roomNumber, name, email, cpf, checkIn, checkOut, guests,
                RatePlans.byCode(planCode), companyCode);
    }

    private static String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw BusinessException.invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asText();
    }

    private static int integer(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw BusinessException.invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asInt();
    }

    private static LocalDate date(JsonNode json, String field) {
        try {
            return LocalDate.parse(text(json, field));
        } catch (DateTimeParseException e) {
            throw BusinessException.invalid("Data inválida em " + field + " (use AAAA-MM-DD)");
        }
    }
}
