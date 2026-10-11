package br.com.loja.checkout.resumo;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Opções disponíveis de um tipo, indexadas pelo código. */
public final class Catalogo<T extends Codificado> {

    private final Map<String, T> porCodigo;

    public Catalogo(List<T> opcoes) {
        this.porCodigo = opcoes.stream().collect(Collectors.toUnmodifiableMap(Codificado::codigo, Function.identity()));
    }

    public Optional<T> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
