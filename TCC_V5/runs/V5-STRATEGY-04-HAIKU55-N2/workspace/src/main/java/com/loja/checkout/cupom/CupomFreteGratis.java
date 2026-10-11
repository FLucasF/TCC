package com.loja.checkout.cupom;

import com.loja.checkout.calculo.ContextoCompra;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class CupomFreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public BigDecimal desconto(ContextoCompra compra) {
        return compra.frete();
    }
}
