package br.com.loja.checkout;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Opções disponíveis de um tipo (entregas, cupons, ...), encontradas pelo código. */
public final class Catalogo<T> {

    private final Map<String, T> porCodigo;

    public Catalogo(Collection<T> opcoes, Function<T, String> codigo) {
        this.porCodigo = opcoes.stream().collect(Collectors.toUnmodifiableMap(codigo, Function.identity()));
    }

    public Optional<T> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
