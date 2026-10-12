package br.com.loja.checkout.comum;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Procura uma opção pelo código que o site mandou. Opção nova entra no
 * catálogo só por existir; nada aqui muda.
 */
public class Catalogo<T extends Codificado> {

    private final Map<String, T> porCodigo;

    public Catalogo(Collection<? extends T> opcoes) {
        this.porCodigo = opcoes.stream().collect(Collectors.toMap(
                Codificado::codigo, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    public Optional<T> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
