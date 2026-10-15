package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.ContextoCalculo;
import java.math.BigDecimal;

public class FreteGratis implements Cupom {
    @Override
    public boolean aplicavel(ContextoCalculo contexto) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(ContextoCalculo contexto) {
        return contexto.getFrete();
    }
}
