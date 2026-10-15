package com.loja.checkout.seguro;

import java.math.BigDecimal;

public class SegurocCentroOeste extends SeguroBase {
    @Override
    protected BigDecimal obterPercentual() {
        return new BigDecimal("0.015");
    }
}
