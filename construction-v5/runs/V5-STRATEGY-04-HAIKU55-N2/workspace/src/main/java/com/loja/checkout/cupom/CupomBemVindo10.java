package com.loja.checkout.cupom;

import com.loja.checkout.calculo.ContextoCompra;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class CupomBemVindo10 implements Cupom {

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public BigDecimal desconto(ContextoCompra compra) {
        return compra.subtotal().multiply(new BigDecimal("0.10"));
    }
}
