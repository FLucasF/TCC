package pousada;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Pedido de reserva já validado quanto ao formato. */
public record BookingRequest(int roomNumber, String guestName, String guestEmail, String guestCpf,
                             LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static BookingRequest parse(String json) {
        JsonNode root;
        try {
            root = MAPPER.readTree(json);
        } catch (Exception e) {
            throw ApiException.invalid("O corpo não é um JSON válido");
        }
        if (root == null || !root.isObject()) {
            throw ApiException.invalid("O corpo deve ser um objeto JSON");
        }
        LocalDate checkIn = date(root, "checkIn");
        LocalDate checkOut = date(root, "checkOut");
        BookingRequest request = new BookingRequest(
                integer(root, "roomNumber"), text(root, "guestName"), text(root, "guestEmail"),
                text(root, "guestCpf"), checkIn, checkOut, integer(root, "guests"), ratePlan(root));
        if (!checkOut.isAfter(checkIn)) {
            throw ApiException.invalid("A saída deve ser depois da entrada");
        }
        if (request.guests < 1) {
            throw ApiException.invalid("É preciso pelo menos um hóspede");
        }
        return request;
    }

    private static String text(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw ApiException.invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asText();
    }

    private static int integer(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw ApiException.invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asInt();
    }

    private static LocalDate date(JsonNode root, String field) {
        try {
            return LocalDate.parse(text(root, field));
        } catch (DateTimeParseException e) {
            throw ApiException.invalid("Data inválida em " + field + " (use AAAA-MM-DD)");
        }
    }

    private static RatePlan ratePlan(JsonNode root) {
        try {
            return RatePlan.valueOf(text(root, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw ApiException.invalid("Plano de tarifa inválido");
        }
    }
}
