package br.com.loja.checkout.dominio;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Lista de opções identificadas por código (modalidades de entrega, cupons, níveis, formas de pagamento).
 * Cada opção nova é só um componente Spring a mais; o catálogo encontra todas automaticamente.
 */
public final class Catalogo<T> {

    private final Map<String, T> porCodigo;

    public Catalogo(Collection<T> opcoes, Function<T, String> codigo) {
        this.porCodigo = opcoes.stream().collect(Collectors.toUnmodifiableMap(codigo, Function.identity()));
    }

    public Optional<T> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
