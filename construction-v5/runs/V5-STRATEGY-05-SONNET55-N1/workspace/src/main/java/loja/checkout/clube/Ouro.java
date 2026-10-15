package loja.checkout.clube;

import loja.checkout.Dinheiro;
import loja.checkout.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Ouro implements NivelClube {

    private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal frete(BigDecimal freteDaEntrega) {
        return Dinheiro.ZERO;
    }

    @Override
    public BigDecimal credito(Pedido pedido) {
        return Dinheiro.percentual(pedido.subtotal(), "0.05");
    }

    @Override
    public boolean brinde(Pedido pedido) {
        return pedido.subtotal().compareTo(LIMITE_BRINDE) > 0;
    }
}
