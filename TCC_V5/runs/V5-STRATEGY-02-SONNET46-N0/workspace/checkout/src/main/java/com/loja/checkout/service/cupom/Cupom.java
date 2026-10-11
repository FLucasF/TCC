package com.loja.checkout.service.cupom;

import java.math.BigDecimal;

public interface Cupom {
    String getCodigo();
    boolean aplicavel(BigDecimal subtotalProdutos);

    /**
     * Returns the coupon discount amount.
     * For FRETEGRATIS, frete value must be passed via context — handled in CupomService.
     */
    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete);
}
