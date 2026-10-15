package br.tcc.checkout.dominio.pagamento;

import java.math.BigDecimal;

public class ResultadoPagamento {
    private final BigDecimal totalFinal;
    private final BigDecimal valorParcela;
    private final BigDecimal ajuste;

    public ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela, BigDecimal ajuste) {
        this.totalFinal = totalFinal;
        this.valorParcela = valorParcela;
        this.ajuste = ajuste;
    }

    public BigDecimal getTotalFinal() {
        return totalFinal;
    }

    public BigDecimal getValorParcela() {
        return valorParcela;
    }

    public BigDecimal getAjuste() {
        return ajuste;
    }
}
