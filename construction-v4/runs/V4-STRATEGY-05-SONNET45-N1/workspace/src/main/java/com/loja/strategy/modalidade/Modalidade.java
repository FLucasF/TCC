package com.loja.strategy.modalidade;

import com.loja.dto.CheckoutRequest;
import java.math.BigDecimal;

public interface Modalidade {
    boolean isDisponivel(BigDecimal pesoTotal);
    BigDecimal calcularFrete(BigDecimal pesoTotal);
    int getPrazoEntregaDias();
}
