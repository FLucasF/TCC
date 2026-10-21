package br.com.marealta.dominio;

import java.math.BigDecimal;

public class PlanoNaoReembolsavel extends PlanoDeTarifa {

    public PlanoNaoReembolsavel() {
        super("NON_REFUNDABLE");
    }

    @Override
    protected BigDecimal desconto(Estadia estadia, BigDecimal diarias) {
        return Dinheiro.percentual(diarias, 15);
    }

    @Override
    public BigDecimal devolucao(BigDecimal valorPago) {
        return Dinheiro.ZERO;
    }
}
