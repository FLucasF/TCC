package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.ContextoCalculo;
import java.math.BigDecimal;

public interface Cupom {
    boolean aplicavel(ContextoCalculo contexto);
    BigDecimal calcularDesconto(ContextoCalculo contexto);
}
