package loja.checkout.cupom;

import java.math.BigDecimal;
import loja.checkout.dominio.Carrinho;
import loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

@Component
public class Bemvindo10 implements Cupom {

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
        return Dinheiro.percentual(carrinho.subtotal(), "0.10");
    }
}
