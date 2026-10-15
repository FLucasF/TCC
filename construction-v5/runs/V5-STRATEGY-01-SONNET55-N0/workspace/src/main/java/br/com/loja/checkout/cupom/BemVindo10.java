package br.com.loja.checkout.cupom;

import br.com.loja.checkout.dominio.Carrinho;
import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class BemVindo10 implements Cupom {

    public String codigo() {
        return "BEMVINDO10";
    }

    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
        return Dinheiro.arredondar(carrinho.subtotal().multiply(new BigDecimal("0.10")));
    }
}
