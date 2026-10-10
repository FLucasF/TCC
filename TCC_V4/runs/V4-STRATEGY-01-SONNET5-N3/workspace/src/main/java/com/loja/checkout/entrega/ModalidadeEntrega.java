package com.loja.checkout.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {

    boolean disponivelPara(BigDecimal pesoTotalKg);

    BigDecimal calcularFrete(BigDecimal pesoTotalKg);

    int prazoDias();
}
