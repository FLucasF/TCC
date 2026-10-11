package com.loja.checkout.domain.cupom;

import java.math.BigDecimal;

/**
 * Uma promocao. O marketing inventa uma nova toda hora: cada uma e uma
 * implementacao com seu codigo, sua condicao e sua forma de descontar.
 */
public interface Cupom {

    /** O codigo do cupom, em maiusculas, exatamente como o cliente digita. */
    String codigo();

    /**
     * Confere se o pedido cumpre a condicao do cupom.
     * Lanca CUPOM_NAO_APLICAVEL quando nao cumpre.
     */
    void validarAplicabilidade(DadosCupom dados);

    /** Valor do desconto, ja arredondado em centavos. */
    BigDecimal desconto(DadosCupom dados);
}
