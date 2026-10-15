package com.loja.checkout.entrega;

import com.loja.checkout.comum.Codificavel;

import java.math.BigDecimal;

/**
 * Uma forma de entrega. O enunciado avisa que quase toda semana entra uma
 * opção nova, cada uma com seu jeito de cobrar, seu prazo e suas limitações.
 * Por isso cada modalidade mora numa classe própria: para somar uma nova
 * basta criar mais um componente, sem mexer em nenhuma condição existente.
 *
 * O peso recebido é o peso total do pedido (soma de peso x quantidade), sem
 * arredondar.
 */
public interface ModalidadeEntrega extends Codificavel {

    /** A modalidade atende um pedido com este peso? (ex.: motoboy só até 5 kg) */
    boolean atende(BigDecimal pesoKg);

    /** Valor do frete para este peso, já em centavos. */
    BigDecimal frete(BigDecimal pesoKg);

    int prazoDias();
}
