package loja.checkout;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Busca de opções pelo código; quem entra no sistema é decidido pelas implementações existentes. */
public final class Catalogo<T extends Codificado> {

    private final Map<String, T> porCodigo = new LinkedHashMap<>();

    public Catalogo(Collection<T> opcoes) {
        opcoes.forEach(o -> porCodigo.put(o.codigo(), o));
    }

    public Optional<T> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
