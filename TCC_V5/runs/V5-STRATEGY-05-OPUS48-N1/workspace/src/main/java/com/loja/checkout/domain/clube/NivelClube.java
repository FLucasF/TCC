package com.loja.checkout.domain.clube;

import com.loja.checkout.domain.comum.ComCodigo;

import java.math.BigDecimal;

/**
 * Um nivel do clube. O enunciado diz que vao criar mais niveis e cada um tera
 * seu conjunto de vantagens, entao cada nivel mora na sua classe e decide as
 * tres vantagens que hoje existem: credito para a proxima compra, se paga
 * frete e se manda brinde.
 */
public interface NivelClube extends ComCodigo {

    /** Credito bruto sobre o valor dos produtos; o servico arredonda. */
    BigDecimal credito(BigDecimal subtotalProdutos);

    /** O frete que aparece no resumo; alguns niveis zeram. */
    BigDecimal freteAplicado(BigDecimal frete);

    boolean brinde(BigDecimal subtotalProdutos);
}
