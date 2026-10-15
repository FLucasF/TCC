package com.loja.checkout.seguro;

import java.math.BigDecimal;

public class SeguroSul extends SeguroBase {
    @Override
    protected BigDecimal obterPercentual() {
        return new BigDecimal("0.01");
    }
}
