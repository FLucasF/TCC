package pousada.service;

import com.fasterxml.jackson.databind.JsonNode;
import pousada.domain.AppException;
import pousada.domain.RatePlan;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public record NewReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                             LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {

    public static NewReservation parse(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw AppException.invalid("O corpo deve ser um objeto JSON");
        }
        LocalDate checkIn = date(body, "checkIn");
        LocalDate checkOut = date(body, "checkOut");
        if (!checkOut.isAfter(checkIn)) {
            throw AppException.invalid("A saída deve ser depois da entrada");
        }
        int guests = integer(body, "guests");
        if (guests < 1) {
            throw AppException.invalid("É preciso pelo menos um hóspede");
        }
        RatePlan plan;
        try {
            plan = RatePlan.valueOf(text(body, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw AppException.invalid("ratePlan inválido");
        }
        return new NewReservation(integer(body, "roomNumber"), text(body, "guestName"), text(body, "guestEmail"),
                text(body, "guestCpf"), checkIn, checkOut, guests, plan);
    }

    private static String text(JsonNode body, String field) {
        JsonNode n = body.get(field);
        if (n == null || !n.isTextual() || n.asText().isBlank()) {
            throw AppException.invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return n.asText();
    }

    private static int integer(JsonNode body, String field) {
        JsonNode n = body.get(field);
        if (n == null || !n.isIntegralNumber() || !n.canConvertToInt()) {
            throw AppException.invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return n.asInt();
    }

    private static LocalDate date(JsonNode body, String field) {
        try {
            return LocalDate.parse(text(body, field));
        } catch (DateTimeParseException e) {
            throw AppException.invalid("Data inválida em " + field);
        }
    }
}
