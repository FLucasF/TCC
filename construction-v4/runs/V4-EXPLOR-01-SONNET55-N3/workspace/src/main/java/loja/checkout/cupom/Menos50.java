package loja.checkout.cupom;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import loja.checkout.comum.Compra;
import loja.checkout.comum.Dinheiro;

@Component
class Menos50 implements Cupom {
    private static final BigDecimal MINIMO = new BigDecimal("300.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(Compra compra) {
        return compra.subtotal().compareTo(MINIMO) >= 0;
    }

    @Override
    public BigDecimal desconto(Compra compra, BigDecimal frete) {
        return Dinheiro.arredondar(new BigDecimal("50.00"));
    }
}
