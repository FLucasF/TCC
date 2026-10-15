package br.com.loja.checkout.catalogo;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Faz a escolha entre os casos de uma variacao a partir do codigo recebido.
 * Registrar um caso novo e acrescentar uma implementacao ao catalogo; nada aqui
 * precisa mudar.
 */
public final class Catalogo<T extends Codificado> {

    private final Map<String, T> porCodigo;

    public Catalogo(Collection<T> casos) {
        Map<String, T> mapa = new LinkedHashMap<>();
        casos.forEach(caso -> mapa.put(caso.codigo(), caso));
        this.porCodigo = Map.copyOf(mapa);
    }

    public Optional<T> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
