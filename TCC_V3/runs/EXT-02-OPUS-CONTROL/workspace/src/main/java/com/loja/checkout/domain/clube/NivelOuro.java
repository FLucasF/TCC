package com.loja.checkout.domain.clube;

import com.loja.checkout.domain.pedido.Dinheiro;
import com.loja.checkout.domain.pedido.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** 5% dos produtos em credito, frete gratis sempre e brinde acima de R$ 500,00 em produtos. */
@Component
public class NivelOuro implements NivelClube {

    private static final BigDecimal CREDITO = new BigDecimal("0.05");
    private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal creditoProximaCompra(Pedido pedido) {
        return Dinheiro.percentual(pedido.subtotalProdutos(), CREDITO);
    }

    @Override
    public boolean freteGratis() {
        return true;
    }

    @Override
    public boolean temBrinde(Pedido pedido) {
        return pedido.subtotalProdutos().compareTo(MINIMO_BRINDE) > 0;
    }
}
