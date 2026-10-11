package com.loja.checkout.service.cupom;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class FreteGratisCupom implements Cupom {

    @Override
    public String getCodigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean isAplicavel(ContextoCupom contexto) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(ContextoCupom contexto) {
        return contexto.freteAtual();
    }
}
