package br.com.loja.checkout.dominio;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Conjunto de opções cadastradas no sistema, buscadas pelo código exato.
 * Para criar uma opção nova basta adicionar uma classe {@code @Component}; ela entra aqui automaticamente.
 */
public final class Catalogo<T extends Codificado> {

    private final Map<String, T> porCodigo;

    public Catalogo(List<T> opcoes) {
        this.porCodigo = opcoes.stream().collect(Collectors.toUnmodifiableMap(Codificado::codigo, Function.identity(),
                (a, b) -> {
                    throw new IllegalStateException("Código repetido: " + a.codigo());
                }));
    }

    public Optional<T> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
