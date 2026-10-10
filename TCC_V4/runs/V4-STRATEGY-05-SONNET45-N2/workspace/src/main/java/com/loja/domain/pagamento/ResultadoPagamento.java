package com.loja.domain.pagamento;

import java.math.BigDecimal;

public class ResultadoPagamento {
    private final BigDecimal ajuste;
    private final BigDecimal totalFinal;
    private final BigDecimal valorParcela;

    public ResultadoPagamento(BigDecimal ajuste, BigDecimal totalFinal, BigDecimal valorParcela) {
        this.ajuste = ajuste;
        this.totalFinal = totalFinal;
        this.valorParcela = valorParcela;
    }

    public BigDecimal getAjuste() {
        return ajuste;
    }

    public BigDecimal getTotalFinal() {
        return totalFinal;
    }

    public BigDecimal getValorParcela() {
        return valorParcela;
    }
}
