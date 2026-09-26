package com.loja.checkout.entrega;

import java.math.BigDecimal;

public interface OpcaoEntrega {

    String codigo();

    int prazoDias();

    boolean disponivelPara(BigDecimal pesoTotalKg);

    BigDecimal custo(BigDecimal pesoTotalKg);
}
