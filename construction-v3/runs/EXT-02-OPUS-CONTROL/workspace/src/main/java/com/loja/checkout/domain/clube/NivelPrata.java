package com.loja.checkout.domain.clube;

import com.loja.checkout.domain.pedido.Dinheiro;
import com.loja.checkout.domain.pedido.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** 2% dos produtos de volta em credito. */
@Component
public class NivelPrata implements NivelClube {

    private static final BigDecimal CREDITO = new BigDecimal("0.02");

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal creditoProximaCompra(Pedido pedido) {
        return Dinheiro.percentual(pedido.subtotalProdutos(), CREDITO);
    }
}
