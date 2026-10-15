package com.loja.checkout.clube;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.dominio.Percentual;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ClubeOuro implements NivelClube {

    private static final Percentual CREDITO = Percentual.de("5");
    private static final BigDecimal PRODUTOS_PARA_BRINDE = Dinheiro.reais("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    /** OURO nao paga frete nunca. */
    @Override
    public BigDecimal frete(Pedido pedido, BigDecimal freteDaModalidade) {
        return Dinheiro.ZERO;
    }

    @Override
    public BigDecimal creditoProximaCompra(Pedido pedido) {
        return CREDITO.sobre(pedido.subtotalProdutos());
    }

    @Override
    public boolean brinde(Pedido pedido) {
        return pedido.subtotalProdutos().compareTo(PRODUTOS_PARA_BRINDE) > 0;
    }
}
