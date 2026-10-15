package br.com.loja.checkout;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** Encontra um caso pelo código, sem sequência de condições. */
public class Catalogo<T> {

    private final Map<String, T> porCodigo = new HashMap<>();

    public Catalogo(Collection<T> casos, Function<T, String> codigo) {
        casos.forEach(c -> porCodigo.put(codigo.apply(c), c));
    }

    public Optional<T> buscar(String codigo) {
        return Optional.ofNullable(codigo == null ? null : porCodigo.get(codigo));
    }
}
