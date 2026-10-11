package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;

public interface Modalidade {
    boolean atende(BigDecimal pesoKg);
    BigDecimal custo(BigDecimal pesoKg);
    int prazoDias();
}
