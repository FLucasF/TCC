package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Identificado;
import java.math.BigDecimal;

/**
 * Forma de entrega. Cada parceria nova é uma implementação nova, com seu jeito
 * de cobrar, seu prazo e suas limitações.
 */
public interface ModalidadeEntrega extends Identificado {

    /** Frete em reais para o peso do pedido, já em centavos. */
    BigDecimal frete(BigDecimal pesoKg);

    int prazoDias();

    /** Se a modalidade atende um pedido com esse peso. */
    default boolean atende(BigDecimal pesoKg) {
        return true;
    }
}
