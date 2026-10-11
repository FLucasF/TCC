package br.com.loja.checkout.dominio;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Opções disponíveis de um tipo (entrega, cupom, nível, pagamento), localizadas pelo código. */
public final class Catalogo<T> {

    private final Map<String, T> porCodigo;

    public Catalogo(Collection<T> opcoes, Function<T, String> codigo) {
        this.porCodigo = opcoes.stream().collect(Collectors.toUnmodifiableMap(codigo, Function.identity()));
    }

    public Optional<T> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
