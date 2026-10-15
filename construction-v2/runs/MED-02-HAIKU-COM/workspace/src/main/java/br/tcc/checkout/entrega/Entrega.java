package br.tcc.checkout.entrega;

import java.math.BigDecimal;

public interface Entrega {
    BigDecimal calcularFrete(Double pesoTotal);

    Integer getPrazoEntregaDias();

    boolean podeAtender(Double pesoTotal);
}
