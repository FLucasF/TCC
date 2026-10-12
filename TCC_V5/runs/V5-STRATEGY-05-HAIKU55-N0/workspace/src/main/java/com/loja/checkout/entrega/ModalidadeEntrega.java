package com.loja.checkout.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {

    String codigo();

    boolean atende(BigDecimal pesoKg);

    BigDecimal frete(BigDecimal pesoKg);

    int prazoDias();
}
