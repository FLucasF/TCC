package br.com.loja.checkout.cupom;

import br.com.loja.checkout.Dinheiro;
import br.com.loja.checkout.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Menos50 implements Cupom {
    private static final BigDecimal MINIMO = Dinheiro.reais("300.00");

    public String codigo() { return "MENOS50"; }

    public boolean aplicavel(Pedido pedido) {
        return pedido.subtotal().compareTo(MINIMO) >= 0;
    }

    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        return Dinheiro.reais("50.00");
    }
}
