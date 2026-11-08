package pousada.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pousada.domain.AppException;
import pousada.domain.ErrorCode;
import pousada.service.NewReservation;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Lê e valida o corpo do POST /reservations. */
final class ReservationRequest {
    private final JsonNode body;

    private ReservationRequest(JsonNode body) {
        this.body = body;
    }

    static NewReservation parse(ObjectMapper mapper, String raw) {
        JsonNode node;
        try {
            node = mapper.readTree(raw);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw invalid("O corpo não é um JSON válido");
        }
        if (node == null || !node.isObject()) {
            throw invalid("O corpo deve ser um objeto JSON");
        }
        return new ReservationRequest(node).build();
    }

    private NewReservation build() {
        LocalDate checkIn = date("checkIn");
        LocalDate checkOut = date("checkOut");
        int guests = integer("guests");
        if (!checkOut.isAfter(checkIn)) {
            throw invalid("A saída deve ser depois da entrada");
        }
        if (guests < 1) {
            throw invalid("É preciso pelo menos um hóspede");
        }
        JsonNode company = body.get("companyCode");
        String companyCode = company != null && company.isTextual() ? company.asText().trim() : null;
        return new NewReservation(integer("roomNumber"), text("guestName"), text("guestEmail"), text("guestCpf"),
                checkIn, checkOut, guests, text("ratePlan"), companyCode);
    }

    private String text(String field) {
        JsonNode node = body.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asText().trim();
    }

    private int integer(String field) {
        JsonNode node = body.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asInt();
    }

    private LocalDate date(String field) {
        try {
            return LocalDate.parse(text(field));
        } catch (DateTimeParseException e) {
            throw invalid("Data inválida em " + field + " (use AAAA-MM-DD)");
        }
    }

    private static AppException invalid(String message) {
        return new AppException(ErrorCode.INVALID_REQUEST, message);
    }
}
