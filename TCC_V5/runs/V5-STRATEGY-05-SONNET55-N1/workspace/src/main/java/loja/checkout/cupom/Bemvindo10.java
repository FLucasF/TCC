package loja.checkout.cupom;

import loja.checkout.Dinheiro;
import loja.checkout.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Bemvindo10 implements Cupom {

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        return Dinheiro.percentual(pedido.subtotal(), "0.10");
    }
}
