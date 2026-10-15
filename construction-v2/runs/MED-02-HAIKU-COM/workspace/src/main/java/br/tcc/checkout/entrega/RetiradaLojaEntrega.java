package br.tcc.checkout.entrega;

import java.math.BigDecimal;

public class RetiradaLojaEntrega implements Entrega {
    @Override
    public BigDecimal calcularFrete(Double pesoTotal) {
        return new BigDecimal("0.00");
    }

    @Override
    public Integer getPrazoEntregaDias() {
        return 1;
    }

    @Override
    public boolean podeAtender(Double pesoTotal) {
        return true;
    }
}
