package br.tcc.checkout.entrega;

import java.math.BigDecimal;

public class MotoplayEntrega implements Entrega {
    private static final double PESO_MAXIMO = 5.0;

    @Override
    public BigDecimal calcularFrete(Double pesoTotal) {
        return new BigDecimal("18.00");
    }

    @Override
    public Integer getPrazoEntregaDias() {
        return 0;
    }

    @Override
    public boolean podeAtender(Double pesoTotal) {
        return pesoTotal <= PESO_MAXIMO;
    }
}
