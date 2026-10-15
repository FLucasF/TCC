package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;

public interface Entrega {
    String codigo();
    BigDecimal frete(BigDecimal pesoKg);
    int prazoDias();
    boolean disponivelPara(BigDecimal pesoKg);
}
