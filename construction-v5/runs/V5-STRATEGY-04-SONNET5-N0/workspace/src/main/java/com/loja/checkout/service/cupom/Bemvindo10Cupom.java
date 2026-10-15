package com.loja.checkout.service.cupom;

import com.loja.checkout.util.Money;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Bemvindo10Cupom implements Cupom {

    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public String getCodigo() {
        return "BEMVINDO10";
    }

    @Override
    public boolean isAplicavel(ContextoCupom contexto) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(ContextoCupom contexto) {
        return Money.round(contexto.subtotalProdutos().multiply(PERCENTUAL));
    }
}
