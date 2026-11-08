package pousada.web;

import java.lang.reflect.Type;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.javalin.json.JsonMapper;

public final class Json {
    public static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public static final JsonMapper JAVALIN_MAPPER = new JsonMapper() {
        @Override
        public String toJsonString(Object obj, Type type) {
            try {
                return MAPPER.writeValueAsString(obj);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public <T> T fromJsonString(String json, Type targetType) {
            try {
                return MAPPER.readValue(json, MAPPER.constructType(targetType));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException(e);
            }
        }
    };

    private Json() {
    }
}
