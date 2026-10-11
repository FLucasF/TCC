package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;

/**
 * Uma forma de entrega. Cada transportadora/opcao tem seu codigo, seu jeito de
 * cobrar o frete, seu prazo e suas limitacoes. Para adicionar uma opcao nova,
 * basta criar uma nova classe que implemente esta interface e marcar com
 * {@code @Component}: ela entra no registro automaticamente.
 */
public interface ModalidadeEntrega {

    /** Codigo usado pelo site, ex.: "EXPRESSA". */
    String codigo();

    /** Prazo de entrega em dias. */
    int prazoDias();

    /**
     * A opcao atende um pedido com este peso? Ex.: motoboy so ate 5 kg.
     * O peso vem sem arredondamento.
     */
    boolean atende(BigDecimal pesoTotalKg);

    /** Valor do frete para este peso (ainda sem arredondar). */
    BigDecimal calcularFrete(BigDecimal pesoTotalKg);
}
