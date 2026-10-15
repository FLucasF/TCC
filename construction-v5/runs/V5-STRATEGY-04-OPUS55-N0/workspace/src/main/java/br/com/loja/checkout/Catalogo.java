package br.com.loja.checkout;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Conjunto de opções identificadas por código (entregas, cupons, níveis, pagamentos).
 * Para criar uma opção nova basta criar uma classe {@code @Component}; ela entra no catálogo sozinha.
 */
public final class Catalogo<T> {

    private final Map<String, T> porCodigo;

    public Catalogo(List<T> opcoes, Function<T, String> codigo) {
        this.porCodigo = opcoes.stream().collect(Collectors.toUnmodifiableMap(codigo, Function.identity()));
    }

    public Optional<T> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
