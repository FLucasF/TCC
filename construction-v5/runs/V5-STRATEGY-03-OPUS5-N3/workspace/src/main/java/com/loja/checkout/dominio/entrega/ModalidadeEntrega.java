package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Identificavel;

import java.math.BigDecimal;

/**
 * Uma opção de entrega. Cada opção é dona do seu jeito de cobrar, do seu prazo e da sua
 * limitação: uma transportadora nova é uma classe nova neste pacote, que só precisa dizer
 * o custo — o arredondamento é igual para todas.
 */
public interface ModalidadeEntrega extends Identificavel {

    /** Custo da entrega para um pedido com este peso, sem arredondar. */
    BigDecimal custo(BigDecimal pesoKg);

    int prazoDias();

    /** Se esta opção atende um pedido com este peso. */
    default boolean atende(BigDecimal pesoKg) {
        return true;
    }

    default BigDecimal frete(BigDecimal pesoKg) {
        return Dinheiro.arredonda(custo(pesoKg));
    }
}
