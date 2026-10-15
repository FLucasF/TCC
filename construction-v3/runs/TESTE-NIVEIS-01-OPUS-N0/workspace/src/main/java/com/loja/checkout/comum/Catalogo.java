package com.loja.checkout.comum;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Catalogo de opcoes registradas no sistema (entregas, cupons, niveis, pagamentos).
 * Cada nova opcao e apenas um bean novo: o catalogo a descobre sozinho.
 */
public abstract class Catalogo<T extends Identificavel> {

    private final Map<String, T> porCodigo;
    private final CodigoErro erroQuandoNaoExiste;

    protected Catalogo(List<T> opcoes, CodigoErro erroQuandoNaoExiste) {
        this.erroQuandoNaoExiste = erroQuandoNaoExiste;
        this.porCodigo = opcoes.stream().collect(Collectors.toMap(
                T::codigo, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    /** Busca pelo codigo exato ou recusa o pedido com o erro configurado. */
    public T buscar(String codigo) {
        T opcao = codigo == null ? null : porCodigo.get(codigo);
        if (opcao == null) {
            throw new CheckoutException(erroQuandoNaoExiste);
        }
        return opcao;
    }
}
