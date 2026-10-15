package br.com.loja.checkout;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** Localiza um caso (entrega, cupom, nível, forma de pagamento) pelo seu código. */
public class Catalogo<T> {

    private final Map<String, T> porCodigo = new HashMap<>();

    public Catalogo(Collection<T> casos, Function<T, String> codigo) {
        casos.forEach(caso -> porCodigo.put(codigo.apply(caso), caso));
    }

    public Optional<T> buscar(String codigo) {
        return Optional.ofNullable(porCodigo.get(codigo));
    }
}
