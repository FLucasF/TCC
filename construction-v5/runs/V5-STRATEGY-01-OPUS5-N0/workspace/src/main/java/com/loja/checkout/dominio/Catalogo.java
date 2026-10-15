package com.loja.checkout.dominio;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Catalogo generico de regras de negocio indexadas por codigo. Os itens sao
 * recolhidos do contexto do Spring, de modo que acrescentar uma regra nova
 * (uma entrega, um cupom, um nivel do clube, uma forma de pagamento) e so
 * criar uma classe nova.
 */
public abstract class Catalogo<T> {

    private final Map<String, T> porCodigo = new LinkedHashMap<>();

    protected Catalogo(Iterable<T> itens, Function<T, String> codigo) {
        for (T item : itens) {
            porCodigo.put(codigo.apply(item), item);
        }
    }

    public Optional<T> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }

    /** Busca pelo codigo ou recusa o pedido com o erro informado. */
    public T exigir(String codigo, CodigoErro erro) {
        return buscar(codigo).orElseThrow(() -> new PedidoRecusadoException(erro));
    }

    public Map<String, T> itens() {
        return Map.copyOf(porCodigo);
    }
}
