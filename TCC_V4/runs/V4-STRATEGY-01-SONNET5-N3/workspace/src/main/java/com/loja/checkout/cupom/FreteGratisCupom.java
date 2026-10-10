package com.loja.checkout.cupom;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component("FRETEGRATIS")
public class FreteGratisCupom implements Cupom {

    @Override
    public boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(ContextoCupom contexto) {
        return contexto.frete();
    }
}
