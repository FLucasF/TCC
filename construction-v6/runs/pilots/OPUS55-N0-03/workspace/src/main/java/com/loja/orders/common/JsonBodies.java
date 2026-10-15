package com.loja.orders.common;

import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.MissingNode;

/**
 * Lê os corpos JSON sem recusar nada: quem decide se um campo faltou ou veio errado é a regra de
 * negócio, na ordem de conferência combinada.
 */
@Component
public class JsonBodies {

    private final JsonMapper mapper;

    public JsonBodies(JsonMapper mapper) {
        this.mapper = mapper;
    }

    /** O corpo como árvore JSON; corpo vazio ou mal formado vira um nó ausente. */
    public JsonNode read(String body) {
        if (body == null || body.isBlank()) {
            return MissingNode.getInstance();
        }
        try {
            JsonNode node = mapper.readTree(body);
            return node == null ? MissingNode.getInstance() : node;
        } catch (JacksonException e) {
            return MissingNode.getInstance();
        }
    }

    /** O texto do campo, ou {@code null} se não veio, não é texto ou está em branco. */
    public static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isString()) {
            return null;
        }
        String text = value.stringValue();
        return text.isBlank() ? null : text;
    }
}
