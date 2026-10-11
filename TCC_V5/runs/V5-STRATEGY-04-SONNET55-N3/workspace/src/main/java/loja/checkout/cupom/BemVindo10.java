package loja.checkout.cupom;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import loja.checkout.Carrinho;
import loja.checkout.Dinheiro;

@Component
class BemVindo10 implements Cupom {

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
        return Dinheiro.percentual(carrinho.subtotal(), "0.10");
    }
}
