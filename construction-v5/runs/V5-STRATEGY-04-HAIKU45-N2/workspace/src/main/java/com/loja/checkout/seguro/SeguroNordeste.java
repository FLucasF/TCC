package com.loja.checkout.seguro;

import java.math.BigDecimal;

public class SeguroNordeste extends SeguroBase {
    @Override
    protected BigDecimal obterPercentual() {
        return new BigDecimal("0.02");
    }
}
