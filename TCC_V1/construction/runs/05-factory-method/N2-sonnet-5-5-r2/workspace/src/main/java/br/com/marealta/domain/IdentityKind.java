package br.com.marealta.domain;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Um jeito de se identificar: os campos que exige e como montar a identificação a partir deles. */
public record IdentityKind(List<String> fields, Function<Map<String, String>, Identity> factory) {
}
