package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/**
 * Uma opção de entrega. Cada parceria nova entra como uma implementação com seu
 * jeito de cobrar, seu prazo e suas limitações.
 */
public interface ModalidadeEntrega {

    String codigo();

    BigDecimal frete(Pedido pedido);

    int prazoDias();

    /** Se a opção atende este pedido (ex.: limite de peso do motoboy). */
    default boolean atende(Pedido pedido) {
        return true;
    }
}
