package br.loja.checkout.cupom;

import br.loja.checkout.Dinheiro;
import br.loja.checkout.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Menos50 implements Cupom {
    private static final BigDecimal MINIMO = Dinheiro.valor("300.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(Pedido pedido, BigDecimal frete) {
        return pedido.subtotal().compareTo(MINIMO) >= 0;
    }

    @Override
    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        return Dinheiro.valor("50.00");
    }
}
