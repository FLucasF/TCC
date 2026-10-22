package pousada.identity;

import pousada.domain.PousadaException;

import java.util.Map;

final class Fields {
    private Fields() {
    }

    static String required(Map<String, String> fields, String name) {
        String value = fields.get(name);
        if (value == null || value.isBlank()) {
            throw PousadaException.invalid("Campo obrigatório: " + name);
        }
        return value;
    }
}
