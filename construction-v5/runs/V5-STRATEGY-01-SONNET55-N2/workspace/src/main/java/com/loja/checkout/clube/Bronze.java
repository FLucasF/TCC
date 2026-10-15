package com.loja.checkout.clube;

import com.loja.checkout.dominio.Carrinho;
import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Bronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }

    @Override
    public BigDecimal freteDevido(BigDecimal frete) {
        return frete;
    }

    @Override
    public BigDecimal creditoProximaCompra(Carrinho carrinho) {
        return Dinheiro.ZERO;
    }

    @Override
    public boolean brinde(Carrinho carrinho) {
        return false;
    }
}
