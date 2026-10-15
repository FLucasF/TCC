package br.com.loja.checkout.dominio;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Guarda as opcoes disponiveis de um tipo e acha a pedida pelo codigo. */
public final class Catalogo<T extends Codificavel> {

    private final Map<String, T> porCodigo;

    public Catalogo(Collection<T> opcoes) {
        this.porCodigo = opcoes.stream().collect(Collectors.toMap(
                Codificavel::codigo, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    public Optional<T> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
