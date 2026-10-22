package pousada;

import java.util.Map;

record Identification(IdentificationType type, Map<String, String> values) {
}
