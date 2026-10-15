package com.loja.checkout.cupom;

import com.loja.checkout.comum.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** R$ 50,00 de desconto nos produtos, só a partir de R$ 300,00 em produtos. */
@Component
public class Menos50 implements Cupom {

    private static final BigDecimal MINIMO = new BigDecimal("300.00");
    private static final BigDecimal DESCONTO = new BigDecimal("50.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(CupomContexto ctx) {
        return ctx.subtotal().compareTo(MINIMO) >= 0;
    }

    @Override
    public BigDecimal desconto(CupomContexto ctx) {
        return Dinheiro.centavos(DESCONTO);
    }
}
