package br.com.mareal.api;

import br.com.mareal.domain.ApiException;
import br.com.mareal.domain.ReservationService.NewReservation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.regex.Pattern;

/** Validates the format of what arrives over HTTP. */
final class RequestParser {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern MONTH = Pattern.compile("\\d{4}-\\d{2}");

    private RequestParser() {}

    static NewReservation reservation(String body) {
        JsonNode root;
        try {
            root = MAPPER.readTree(body);
        } catch (Exception e) {
            throw ApiException.invalid("O corpo da requisição não é um JSON válido.");
        }
        if (root == null || !root.isObject()) {
            throw ApiException.invalid("O corpo da requisição deve ser um objeto JSON.");
        }
        LocalDate checkIn = date(root, "checkIn");
        LocalDate checkOut = date(root, "checkOut");
        JsonNode guests = root.get("guests");
        if (guests == null || !guests.isIntegralNumber() || !guests.canConvertToInt()) {
            throw ApiException.invalid("Campo guests ausente ou inválido.");
        }
        JsonNode room = root.get("roomNumber");
        if (room == null || !room.isIntegralNumber() || !room.canConvertToInt()) {
            throw ApiException.invalid("Campo roomNumber ausente ou inválido.");
        }
        String guestName = text(root, "guestName");
        String guestEmail = text(root, "guestEmail");
        String guestCpf = text(root, "guestCpf");
        String ratePlan = text(root, "ratePlan");
        if (!checkOut.isAfter(checkIn)) {
            throw ApiException.invalid("A saída deve ser depois da entrada.");
        }
        if (guests.intValue() < 1) {
            throw ApiException.invalid("A reserva precisa de pelo menos um hóspede.");
        }
        JsonNode company = root.get("companyCode");
        String companyCode = company != null && company.isTextual() ? company.textValue() : null;
        return new NewReservation(room.intValue(), guestName, guestEmail, guestCpf, checkIn, checkOut,
                guests.intValue(), ratePlan, companyCode);
    }

    static LocalDate day(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeException | NullPointerException e) {
            throw ApiException.invalid("Data inválida; use o formato 2027-07-10.");
        }
    }

    static YearMonth month(String value) {
        if (value == null || !MONTH.matcher(value).matches()) {
            throw ApiException.invalid("Mês inválido; use o formato 2027-07.");
        }
        try {
            return YearMonth.parse(value);
        } catch (DateTimeException e) {
            throw ApiException.invalid("Mês inválido; use o formato 2027-07.");
        }
    }

    private static String text(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || !node.isTextual() || node.textValue().isBlank()) {
            throw ApiException.invalid("Campo " + field + " ausente ou inválido.");
        }
        return node.textValue();
    }

    private static LocalDate date(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || !node.isTextual()) {
            throw ApiException.invalid("Campo " + field + " ausente ou inválido.");
        }
        return day(node.textValue());
    }
}
