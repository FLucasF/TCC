package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;

public interface OpcaoEntrega {

    String codigo();

    boolean disponivel(BigDecimal pesoTotalKg);

    BigDecimal custo(BigDecimal pesoTotalKg);

    int prazoDias();
}
