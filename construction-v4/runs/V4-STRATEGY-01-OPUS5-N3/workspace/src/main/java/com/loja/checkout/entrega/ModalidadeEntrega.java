package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/**
 * Uma opcao de entrega. Cada parceria tem seu jeito de cobrar, seu prazo e
 * suas limitacoes, e cada uma mora na sua propria implementacao.
 */
public interface ModalidadeEntrega {

    String codigo();

    /** Se a opcao atende este pedido (ex.: motoboy so ate 5 kg). */
    boolean atende(Pedido pedido);

    /** Frete cobrado, arredondado para centavos. */
    BigDecimal frete(Pedido pedido);

    int prazoDias();
}
