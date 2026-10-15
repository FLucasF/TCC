package com.loja.checkout.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {

    String codigo();

    boolean disponivel(BigDecimal pesoKg);

    BigDecimal custo(BigDecimal pesoKg);

    int prazoDias();
}
