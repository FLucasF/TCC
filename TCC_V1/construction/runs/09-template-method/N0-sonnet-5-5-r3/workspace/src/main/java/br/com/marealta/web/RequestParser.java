package br.com.marealta.web;

import br.com.marealta.domain.RatePlan;
import br.com.marealta.service.ApiException;
import br.com.marealta.service.BookingRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/** Validates the POST /reservations body. */
final class RequestParser {

    private static final Pattern ISO_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private RequestParser() {}

    static BookingRequest parseBooking(ObjectMapper mapper, String body) {
        JsonNode json;
        try {
            json = mapper.readTree(body);
        } catch (Exception e) {
            throw ApiException.invalid("O corpo não é um JSON válido");
        }
        if (json == null || !json.isObject()) throw ApiException.invalid("O corpo deve ser um objeto JSON");

        JsonNode room = json.get("roomNumber");
        if (room == null || !room.isIntegralNumber() || !room.canConvertToInt()) {
            throw ApiException.invalid("Campo 'roomNumber' ausente ou inválido");
        }
        JsonNode guests = json.get("guests");
        if (guests == null || !guests.isIntegralNumber() || !guests.canConvertToInt()) {
            throw ApiException.invalid("Campo 'guests' ausente ou inválido");
        }
        String name = text(json, "guestName");
        String email = text(json, "guestEmail");
        String cpf = text(json, "guestCpf");
        LocalDate checkIn = date(json, "checkIn");
        LocalDate checkOut = date(json, "checkOut");
        RatePlan plan;
        try {
            plan = RatePlan.valueOf(text(json, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw ApiException.invalid("Campo 'ratePlan' inválido");
        }
        if (!checkOut.isAfter(checkIn)) throw ApiException.invalid("A saída deve ser depois da entrada");
        if (guests.intValue() < 1) throw ApiException.invalid("É preciso ao menos um hóspede");
        return new BookingRequest(room.intValue(), name, email, cpf, checkIn, checkOut, guests.intValue(), plan);
    }

    private static String text(JsonNode json, String field) {
        JsonNode n = json.get(field);
        if (n == null || !n.isTextual() || n.textValue().isBlank()) {
            throw ApiException.invalid("Campo '" + field + "' ausente ou inválido");
        }
        return n.textValue();
    }

    private static LocalDate date(JsonNode json, String field) {
        String value = text(json, field);
        try {
            if (!ISO_DATE.matcher(value).matches()) throw new DateTimeParseException("format", value, 0);
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid("Campo '" + field + "' deve estar no formato AAAA-MM-DD");
        }
    }
}
