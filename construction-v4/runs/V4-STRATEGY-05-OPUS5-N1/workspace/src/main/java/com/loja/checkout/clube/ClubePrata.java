package com.loja.checkout.clube;

import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.dominio.Percentual;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ClubePrata implements NivelClube {

    private static final Percentual CREDITO = Percentual.de("2");

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal frete(Pedido pedido, BigDecimal freteDaModalidade) {
        return freteDaModalidade;
    }

    @Override
    public BigDecimal creditoProximaCompra(Pedido pedido) {
        return CREDITO.sobre(pedido.subtotalProdutos());
    }

    @Override
    public boolean brinde(Pedido pedido) {
        return false;
    }
}
