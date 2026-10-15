package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/**
 * Uma opcao de entrega da loja.
 *
 * Para adicionar uma transportadora nova basta criar uma classe que implemente
 * esta interface e anota-la com @Component: ela passa a ser aceita pelo servico
 * automaticamente, sem mexer em nenhuma outra classe.
 */
public interface ModalidadeEntrega {

    /** Codigo recebido no campo "modalidadeEntrega" (ex.: EXPRESSA). */
    String codigo();

    /** Prazo em dias mostrado no resumo. */
    int prazoEntregaDias();

    /** Valor do frete, sem arredondar (o servico arredonda para centavos). */
    BigDecimal calcularFrete(Pedido pedido);

    /** Se a opcao atende este pedido (ex.: motoboy so ate 5 kg). */
    default boolean atende(Pedido pedido) {
        return true;
    }
}
