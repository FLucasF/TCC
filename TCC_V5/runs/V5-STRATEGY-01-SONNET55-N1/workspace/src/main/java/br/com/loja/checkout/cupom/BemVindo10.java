package br.com.loja.checkout.cupom;

import br.com.loja.checkout.Dinheiro;
import br.com.loja.checkout.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
class BemVindo10 implements Cupom {

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        return Dinheiro.percentual(pedido.subtotal(), "0.10");
    }
}
