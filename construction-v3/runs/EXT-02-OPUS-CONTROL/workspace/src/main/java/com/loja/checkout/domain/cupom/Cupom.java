package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.pedido.Pedido;
import java.math.BigDecimal;

/**
 * Uma promocao da loja. Cada cupom novo do marketing e uma implementacao
 * anotada com @Component, sem mexer no calculo do resumo.
 */
public interface Cupom {

    /** Codigo digitado pelo cliente, sempre em maiusculas. */
    String codigo();

    /** Desconto em reais, arredondado para centavos. */
    BigDecimal desconto(Pedido pedido, BigDecimal frete);

    /** Condicoes da promocao (valor minimo, itens exigidos, etc.). */
    default boolean aplicavel(Pedido pedido, BigDecimal frete) {
        return true;
    }
}
