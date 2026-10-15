package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;

/**
 * Uma forma de entrega. Cada parceria nova (quase toda semana entra uma) e uma
 * implementacao: traz seu jeito de cobrar o frete, seu prazo e suas limitacoes,
 * tudo num lugar so. Registrar a nova e expor o {@link #codigo()}.
 */
public interface ModalidadeEntrega {

    /** O codigo enviado pelo site (ex.: EXPRESSA). */
    String codigo();

    /** Frete para o peso informado, ja arredondado em centavos. */
    BigDecimal frete(BigDecimal pesoKg);

    /** Prazo de entrega em dias. */
    int prazoDias();

    /**
     * Confere se esta modalidade atende um pedido com este peso.
     * Lanca MODALIDADE_INDISPONIVEL quando nao atende.
     */
    void validarDisponibilidade(BigDecimal pesoKg);
}
