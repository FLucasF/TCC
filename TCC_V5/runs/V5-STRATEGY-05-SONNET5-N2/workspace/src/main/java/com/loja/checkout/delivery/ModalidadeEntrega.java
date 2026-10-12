package com.loja.checkout.delivery;

import java.math.BigDecimal;

public interface ModalidadeEntrega {

    String codigo();

    boolean disponivelPara(BigDecimal pesoKg);

    BigDecimal calcularFrete(BigDecimal pesoKg);

    int prazoEntregaDias();
}
