package loja.checkout.cupom;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import loja.checkout.comum.Compra;
import loja.checkout.comum.Dinheiro;

@Component
class BemVindo10 implements Cupom {
    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public BigDecimal desconto(Compra compra, BigDecimal frete) {
        return Dinheiro.percentual(compra.subtotal(), new BigDecimal("0.10"));
    }
}
