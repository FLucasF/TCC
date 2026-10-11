package br.com.loja.checkout.cupom;

import br.com.loja.checkout.resumo.Carrinho;
import br.com.loja.checkout.resumo.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class BemVindo10 implements Cupom {

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public boolean aplicavel(Carrinho carrinho) {
        return true;
    }

    @Override
    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
        return Dinheiro.percentual(carrinho.subtotal(), "10");
    }
}
