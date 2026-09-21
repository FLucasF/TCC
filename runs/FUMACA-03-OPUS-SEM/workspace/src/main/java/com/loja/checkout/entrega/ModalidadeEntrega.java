package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/**
 * Uma opcao de entrega da loja.
 *
 * <p>Para colocar no ar uma transportadora nova basta criar uma classe que implemente esta
 * interface e anota-la com {@code @Component}: ela passa a ser reconhecida pelo servico sem
 * nenhuma outra alteracao.
 */
public interface ModalidadeEntrega {

    /** Codigo enviado pelo site, em letras maiusculas (ex.: EXPRESSA). */
    String codigo();

    /** Prazo de entrega em dias. */
    int prazoDias();

    /** Frete cobrado pelo pedido, em reais (o servico arredonda para centavos). */
    BigDecimal calcularFrete(Pedido pedido);

    /** Se a modalidade atende este pedido (ex.: limite de peso do motoboy). */
    default boolean atende(Pedido pedido) {
        return true;
    }
}
