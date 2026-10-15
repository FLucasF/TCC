package com.loja.resumo.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {

    String codigo();

    int prazoDias();

    boolean disponivelPara(BigDecimal pesoKg);

    BigDecimal calcularFrete(BigDecimal pesoKg);
}
