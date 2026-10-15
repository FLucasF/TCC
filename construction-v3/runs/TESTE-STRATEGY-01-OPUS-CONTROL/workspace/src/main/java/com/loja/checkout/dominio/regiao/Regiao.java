package com.loja.checkout.dominio.regiao;

import java.math.BigDecimal;

/**
 * Regiao do cliente. A conta do imposto e a mesma para todas as regioes
 * (percentual sobre os produtos ja com o desconto do cupom); so a aliquota muda.
 */
public enum Regiao {

    SUDESTE("12"),
    SUL("11"),
    CENTRO_OESTE("9"),
    NORTE("7"),
    NORDESTE("7");

    private final BigDecimal aliquotaPercentual;

    Regiao(String aliquotaPercentual) {
        this.aliquotaPercentual = new BigDecimal(aliquotaPercentual);
    }

    public BigDecimal aliquotaPercentual() {
        return aliquotaPercentual;
    }
}
