package br.com.loja.checkout.cupom;

import br.com.loja.checkout.Carrinho;
import br.com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class BemVindo10 implements Cupom {

    public String codigo() {
        return "BEMVINDO10";
    }

    public boolean aplicavel(Carrinho carrinho) {
        return true;
    }

    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
        return Dinheiro.percentual(carrinho.subtotal(), "0.10");
    }
}
