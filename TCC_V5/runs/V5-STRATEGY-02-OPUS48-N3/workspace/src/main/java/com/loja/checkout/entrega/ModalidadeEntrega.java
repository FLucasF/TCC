package com.loja.checkout.entrega;

import com.loja.checkout.comum.Codificavel;

import java.math.BigDecimal;

/**
 * Uma forma de entrega. Cada opção tem sua fórmula de frete, seu prazo e sua
 * regra de disponibilidade. Opção nova = uma classe nova implementando isto.
 */
public interface ModalidadeEntrega extends Codificavel {

    /** Frete já arredondado para centavos, a partir do peso total do pedido. */
    BigDecimal frete(BigDecimal pesoKg);

    /** Se a opção atende um pedido com este peso (ex.: motoboy só até 5 kg). */
    boolean atende(BigDecimal pesoKg);

    int prazoDias();
}
