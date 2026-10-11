package com.loja.checkout.clube;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Prata implements NivelClube {

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal frete(BigDecimal freteDaModalidade) {
        return freteDaModalidade;
    }

    @Override
    public BigDecimal credito(Pedido pedido) {
        return Dinheiro.percentual(pedido.subtotalProdutos(), "0.02");
    }

    @Override
    public boolean brinde(Pedido pedido) {
        return false;
    }
}
