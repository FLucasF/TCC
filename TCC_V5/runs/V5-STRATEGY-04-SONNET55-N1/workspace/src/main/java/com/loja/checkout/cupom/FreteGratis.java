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
    public boolean aplicavel(ContextoCupom pedido) {
        return true;
    }

    @Override
    public BigDecimal desconto(ContextoCupom pedido) {
        return pedido.frete();
    }
}
