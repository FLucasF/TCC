package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Codificado;

import java.math.BigDecimal;

/**
 * Uma forma de entrega. Cada forma tem sua maneira de cobrar o frete, seu prazo e
 * suas limitações, então cada uma mora na sua própria classe. Entregas novas
 * entram só adicionando mais uma implementação.
 */
public interface ModalidadeEntrega extends Codificado {

    /** O frete da modalidade para um pedido com este peso total (em kg). */
    BigDecimal frete(BigDecimal pesoKg);

    /** Prazo de entrega, em dias. */
    int prazoDias();

    /** Se a modalidade atende um pedido com este peso (ex.: motoboy só até 5 kg). */
    default boolean atende(BigDecimal pesoKg) {
        return true;
    }
}
