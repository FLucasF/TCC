package br.com.loja.checkout.cupom;

import br.com.loja.checkout.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Menos50 implements Cupom {

    private static final BigDecimal DESCONTO = Dinheiro.reais("50.00");
    private static final BigDecimal COMPRA_MINIMA = Dinheiro.reais("300.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(ContextoCupom contexto) {
        return contexto.subtotal().compareTo(COMPRA_MINIMA) >= 0;
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return DESCONTO;
    }
}
