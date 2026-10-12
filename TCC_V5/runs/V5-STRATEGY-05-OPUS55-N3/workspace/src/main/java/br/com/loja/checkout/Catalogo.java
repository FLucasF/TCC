package br.com.loja.checkout;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Mapa código → implementação, montado a partir das implementações registradas. */
public final class Catalogo<T extends Codificado> {

    private final Map<String, T> porCodigo;

    public Catalogo(List<T> itens) {
        this.porCodigo = itens.stream().collect(Collectors.toUnmodifiableMap(Codificado::codigo, Function.identity()));
    }

    public Optional<T> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
