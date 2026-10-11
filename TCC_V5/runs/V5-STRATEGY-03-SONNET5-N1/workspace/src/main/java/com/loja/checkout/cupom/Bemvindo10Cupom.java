package com.loja.checkout.cupom;

import com.loja.checkout.comum.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Bemvindo10Cupom implements Cupom {

    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(ContextoCupom contexto) {
        return Dinheiro.arredondar(contexto.subtotalProdutos().multiply(PERCENTUAL));
    }
}
