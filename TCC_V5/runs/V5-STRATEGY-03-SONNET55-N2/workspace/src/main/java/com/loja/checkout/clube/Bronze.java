package com.loja.checkout.clube;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Bronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }

    @Override
    public BigDecimal frete(BigDecimal freteDaModalidade) {
        return freteDaModalidade;
    }

    @Override
    public BigDecimal credito(Pedido pedido) {
        return Dinheiro.ZERO;
    }

    @Override
    public boolean brinde(Pedido pedido) {
        return false;
    }
}
