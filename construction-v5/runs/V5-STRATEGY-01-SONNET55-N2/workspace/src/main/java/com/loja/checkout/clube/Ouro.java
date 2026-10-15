package com.loja.checkout.clube;

import com.loja.checkout.dominio.Carrinho;
import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Ouro implements NivelClube {

    private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal freteDevido(BigDecimal frete) {
        return Dinheiro.ZERO;
    }

    @Override
    public BigDecimal creditoProximaCompra(Carrinho carrinho) {
        return Dinheiro.percentual(carrinho.subtotal(), new BigDecimal("0.05"));
    }

    @Override
    public boolean brinde(Carrinho carrinho) {
        return carrinho.subtotal().compareTo(MINIMO_BRINDE) > 0;
    }
}
