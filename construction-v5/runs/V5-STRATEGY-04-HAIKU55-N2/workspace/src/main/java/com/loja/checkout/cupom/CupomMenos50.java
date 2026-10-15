package com.loja.checkout.cupom;

import com.loja.checkout.calculo.ContextoCompra;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class CupomMenos50 implements Cupom {

    private static final BigDecimal VALOR_MINIMO = new BigDecimal("300");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public BigDecimal desconto(ContextoCompra compra) {
        return new BigDecimal("50.00");
    }

    @Override
    public boolean aplicavel(ContextoCompra compra) {
        return compra.subtotal().compareTo(VALOR_MINIMO) >= 0;
    }
}
