package com.pousadamaretalta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class Json {
    public static final ObjectMapper MAPPER = new ObjectMapper();

    private Json() {}

    public static JsonNode parseObject(String body) {
        try {
            JsonNode n = MAPPER.readTree(body);
            if (n == null || !n.isObject()) throw ApiException.invalid("O corpo deve ser um objeto JSON");
            return n;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw ApiException.invalid("O corpo não é um JSON válido");
        }
    }

    /** Texto não vazio; null se ausente/null/vazio; 400 se não for texto. */
    public static String optionalText(JsonNode body, String field) {
        JsonNode n = body.get(field);
        if (n == null || n.isNull()) return null;
        if (!n.isTextual()) throw ApiException.invalid("O campo " + field + " deve ser texto");
        String s = n.asText().trim();
        return s.isEmpty() ? null : s;
    }

    public static String requiredText(JsonNode body, String field) {
        String s = optionalText(body, field);
        if (s == null) throw ApiException.invalid("O campo " + field + " é obrigatório");
        return s;
    }

    public static LocalDate requiredDate(JsonNode body, String field) {
        String s = requiredText(body, field);
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid("O campo " + field + " deve estar no formato AAAA-MM-DD");
        }
    }

    public static int requiredInt(JsonNode body, String field) {
        JsonNode n = body.get(field);
        if (n == null || n.isNull()) throw ApiException.invalid("O campo " + field + " é obrigatório");
        if (!n.isIntegralNumber() || !n.canConvertToInt()) {
            throw ApiException.invalid("O campo " + field + " deve ser um número inteiro");
        }
        return n.asInt();
    }
}
