package loja.checkout.cupom;

import java.math.BigDecimal;
import loja.checkout.dominio.Dinheiro;
import loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

@Component
class BemVindo10 implements Cupom {
    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public boolean aplicavel(Pedido pedido) {
        return true;
    }

    @Override
    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        return Dinheiro.arredondar(pedido.subtotal().multiply(new BigDecimal("0.10")));
    }
}
