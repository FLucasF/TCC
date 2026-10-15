package com.loja.checkout.cupom;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FreteGratisCupom implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(ContextoCupom contexto) {
        return contexto.freteAntesDoCupom();
    }
}
