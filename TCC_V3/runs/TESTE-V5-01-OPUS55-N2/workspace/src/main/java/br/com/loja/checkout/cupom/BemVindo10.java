package br.com.loja.checkout.cupom;

import br.com.loja.checkout.Carrinho;
import br.com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class BemVindo10 implements Cupom {

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
        return Dinheiro.percentual(carrinho.subtotal(), "10");
    }
}
