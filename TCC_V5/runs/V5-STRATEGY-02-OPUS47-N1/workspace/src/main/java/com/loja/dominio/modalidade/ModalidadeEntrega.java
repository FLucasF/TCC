package com.loja.dominio.modalidade;

import java.math.BigDecimal;

public interface ModalidadeEntrega {
    BigDecimal calcularFrete(BigDecimal pesoKg);
    int prazoDias();
    boolean atende(BigDecimal pesoKg);
}
