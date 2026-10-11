package loja.checkout.cupom;

import java.math.BigDecimal;
import loja.checkout.Carrinho;
import loja.checkout.Dinheiro;
import org.springframework.stereotype.Component;

@Component
public class BemVindo10 implements Cupom {
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
        return Dinheiro.percentual(carrinho.subtotal(), "0.10");
    }
}
