package com.loja.checkout.enums;

import java.math.BigDecimal;

public enum Cupom {
    BEMVINDO10(TipoCupom.PERCENTUAL_PRODUTOS, new BigDecimal("0.10"), BigDecimal.ZERO),
    MENOS50(TipoCupom.VALOR_FIXO_PRODUTOS, new BigDecimal("50.00"), new BigDecimal("300.00")),
    FRETEGRATIS(TipoCupom.FRETE_GRATIS, BigDecimal.ZERO, BigDecimal.ZERO),
    LEVE3PAGUE2(TipoCupom.LEVE3PAGUE2, BigDecimal.ZERO, BigDecimal.ZERO);

    private final TipoCupom tipo;
    private final BigDecimal valor;
    private final BigDecimal valorMinimoCompra;

    Cupom(TipoCupom tipo, BigDecimal valor, BigDecimal valorMinimoCompra) {
        this.tipo = tipo;
        this.valor = valor;
        this.valorMinimoCompra = valorMinimoCompra;
    }

    public TipoCupom getTipo() {
        return tipo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public BigDecimal getValorMinimoCompra() {
        return valorMinimoCompra;
    }

    public enum TipoCupom {
        PERCENTUAL_PRODUTOS,
        VALOR_FIXO_PRODUTOS,
        FRETE_GRATIS,
        LEVE3PAGUE2
    }
}
