package com.loja.checkout.domain.modalidade;

import java.math.BigDecimal;

public interface Modalidade {
    BigDecimal calcularFrete(BigDecimal pesoTotalKg);
    int obterPrazoEntrega();
    boolean estaDisponivel(BigDecimal pesoTotalKg);
    String obterCodigo();
}
