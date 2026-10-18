package shop.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.core.JsonGenerator;

public final class Json {
    public static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    private Json() {
    }

    public static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public static <T> T read(String text, Class<T> type) {
        try {
            return MAPPER.readValue(text, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Parses a request body; anything that is not valid JSON becomes a missing node. */
    public static JsonNode parseLenient(String text) {
        if (text == null || text.isBlank()) {
            return MissingNode.getInstance();
        }
        try {
            return MAPPER.readTree(text);
        } catch (JsonProcessingException e) {
            return MissingNode.getInstance();
        }
    }
}
