package com.loja.checkout.entrega;

import java.math.BigDecimal;

/**
 * Uma forma de entrega. Quase toda semana entra uma nova: cada uma é uma classe
 * própria com seu jeito de cobrar, seu prazo e suas limitações. Para adicionar
 * uma modalidade, basta criar uma classe que implemente esta interface.
 */
public interface ModalidadeEntrega {

    String codigo();

    BigDecimal frete(BigDecimal pesoKg);

    int prazoDias();

    /** Se esta modalidade atende um pedido com o peso informado (ex.: motoboy só até 5 kg). */
    default boolean disponivel(BigDecimal pesoKg) {
        return true;
    }
}
