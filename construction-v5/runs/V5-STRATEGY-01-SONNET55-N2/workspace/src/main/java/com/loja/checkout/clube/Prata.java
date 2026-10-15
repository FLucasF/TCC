package com.loja.checkout.clube;

import com.loja.checkout.dominio.Carrinho;
import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Prata implements NivelClube {

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal freteDevido(BigDecimal frete) {
        return frete;
    }

    @Override
    public BigDecimal creditoProximaCompra(Carrinho carrinho) {
        return Dinheiro.percentual(carrinho.subtotal(), new BigDecimal("0.02"));
    }

    @Override
    public boolean brinde(Carrinho carrinho) {
        return false;
    }
}
