package com.loja.checkout.clube;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** So o cadastro: nao ganha nada. */
@Component
public class ClubeBronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }

    @Override
    public BigDecimal frete(Pedido pedido, BigDecimal freteDaModalidade) {
        return freteDaModalidade;
    }

    @Override
    public BigDecimal creditoProximaCompra(Pedido pedido) {
        return Dinheiro.ZERO;
    }

    @Override
    public boolean brinde(Pedido pedido) {
        return false;
    }
}
