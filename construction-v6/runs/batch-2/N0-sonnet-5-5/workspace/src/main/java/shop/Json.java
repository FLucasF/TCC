package shop;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.JsonGenerator;

final class Json {
    static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    private Json() {}

    static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static <T> T read(String text, Class<T> type) {
        try {
            return MAPPER.readValue(text, type);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Lê o corpo; devolve null se for vazio ou não for JSON. */
    static JsonNode tree(String text) {
        try {
            return text == null || text.isBlank() ? null : MAPPER.readTree(text);
        } catch (Exception e) {
            return null;
        }
    }
}
