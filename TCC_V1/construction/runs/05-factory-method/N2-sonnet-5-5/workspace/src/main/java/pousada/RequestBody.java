package pousada;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class RequestBody {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final JsonNode node;

    private RequestBody(JsonNode node) { this.node = node; }

    public static RequestBody parse(String json) {
        try {
            JsonNode node = MAPPER.readTree(json == null ? "" : json);
            if (node == null || !node.isObject()) {
                throw ApiException.invalidRequest("O corpo deve ser um objeto JSON");
            }
            return new RequestBody(node);
        } catch (JsonProcessingException e) {
            throw ApiException.invalidRequest("O corpo não é um JSON válido");
        }
    }

    /** O campo veio com algum valor (texto em branco conta como ausente). */
    public boolean has(String key) {
        JsonNode value = node.get(key);
        return value != null && !value.isNull() && !(value.isTextual() && value.asText().isBlank());
    }

    public String text(String key) {
        if (!has(key)) throw ApiException.invalidRequest("Campo obrigatório: " + key);
        JsonNode value = node.get(key);
        if (!value.isTextual()) throw ApiException.invalidRequest("O campo " + key + " deve ser texto");
        return value.asText().trim();
    }

    public int integer(String key) {
        if (!has(key)) throw ApiException.invalidRequest("Campo obrigatório: " + key);
        JsonNode value = node.get(key);
        if (!value.isIntegralNumber() || !value.canConvertToInt()) {
            throw ApiException.invalidRequest("O campo " + key + " deve ser um número inteiro");
        }
        return value.asInt();
    }

    public LocalDate date(String key) {
        try {
            return LocalDate.parse(text(key));
        } catch (DateTimeParseException e) {
            throw ApiException.invalidRequest("O campo " + key + " deve ter o formato 2027-07-10");
        }
    }

    public <E extends Enum<E>> E enumeration(String key, Class<E> type) {
        String value = text(key);
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            throw ApiException.invalidRequest("Valor inválido para " + key + ": " + value);
        }
    }
}
