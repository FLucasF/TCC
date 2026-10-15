package com.loja.checkout.entrega;

import java.math.BigDecimal;

/**
 * Forma de entrega. Cada opcao tem seu jeito de cobrar, seu prazo e suas
 * limitacoes, e opcoes novas entram quase toda semana. Por isso cada modalidade
 * mora numa classe propria: adicionar uma transportadora e criar uma nova
 * implementacao, sem mexer nas demais nem numa cadeia de condicoes.
 */
public interface ModalidadeEntrega {

    String codigo();

    int prazoDias();

    BigDecimal frete(BigDecimal pesoKg);

    /** Se esta modalidade atende um pedido com esse peso. */
    default boolean atende(BigDecimal pesoKg) {
        return true;
    }
}
