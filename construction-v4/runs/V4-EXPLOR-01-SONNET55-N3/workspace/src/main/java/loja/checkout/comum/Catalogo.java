package loja.checkout.comum;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class Catalogo<T extends Codigo> {
    private final Map<String, T> porCodigo;

    public Catalogo(Collection<T> casos) {
        this.porCodigo = casos.stream().collect(Collectors.toMap(Codigo::codigo, Function.identity()));
    }

    public T buscar(String codigo, String erro) {
        T caso = porCodigo.get(codigo);
        if (caso == null) {
            throw new RecusaException(erro);
        }
        return caso;
    }
}
