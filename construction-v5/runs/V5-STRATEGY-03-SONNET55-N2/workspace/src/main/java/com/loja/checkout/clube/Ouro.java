package com.loja.checkout.clube;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Ouro implements NivelClube {

    private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal frete(BigDecimal freteDaModalidade) {
        return Dinheiro.ZERO;
    }

    @Override
    public BigDecimal credito(Pedido pedido) {
        return Dinheiro.percentual(pedido.subtotalProdutos(), "0.05");
    }

    @Override
    public boolean brinde(Pedido pedido) {
        return pedido.subtotalProdutos().compareTo(LIMITE_BRINDE) > 0;
    }
}
