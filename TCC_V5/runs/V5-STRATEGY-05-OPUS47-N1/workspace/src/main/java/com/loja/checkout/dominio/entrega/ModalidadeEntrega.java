package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {
    boolean atende(BigDecimal pesoKg);

    Frete calcular(BigDecimal pesoKg);
}
