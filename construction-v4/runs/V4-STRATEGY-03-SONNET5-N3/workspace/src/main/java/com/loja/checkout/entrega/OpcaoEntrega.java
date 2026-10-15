package com.loja.checkout.entrega;

import java.math.BigDecimal;

public interface OpcaoEntrega {

    BigDecimal custo(BigDecimal pesoKg);

    int prazoDias();

    boolean disponivel(BigDecimal pesoKg);
}
