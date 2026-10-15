package br.com.loja.checkout.cupom;

import br.com.loja.checkout.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BemVindo10 implements Cupom {

    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return Dinheiro.percentual(contexto.subtotal(), PERCENTUAL);
    }
}
