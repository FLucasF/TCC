package com.loja.checkout.cupom;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean aplicavel(ContextoDesconto contexto) {
        return true;
    }

    @Override
    public BigDecimal desconto(ContextoDesconto contexto) {
        return contexto.frete();
    }
}
