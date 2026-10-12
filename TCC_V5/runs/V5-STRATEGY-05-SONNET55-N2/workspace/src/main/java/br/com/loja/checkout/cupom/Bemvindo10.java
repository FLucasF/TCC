package br.com.loja.checkout.cupom;

import br.com.loja.checkout.Dinheiro;
import br.com.loja.checkout.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Bemvindo10 implements Cupom {
    public String codigo() { return "BEMVINDO10"; }

    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        return Dinheiro.percentual(pedido.subtotal(), "0.10");
    }
}
