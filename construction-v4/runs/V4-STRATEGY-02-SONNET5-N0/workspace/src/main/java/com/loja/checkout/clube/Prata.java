package com.loja.checkout.clube;

import com.loja.checkout.service.PedidoContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Prata implements NivelClube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.02");

    @Override
    public String getCodigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal calcularCredito(PedidoContext pedido) {
        return pedido.subtotalProdutos().multiply(PERCENTUAL_CREDITO);
    }

    @Override
    public boolean freteGratis() {
        return false;
    }

    @Override
    public boolean temDireitoABrinde(PedidoContext pedido) {
        return false;
    }
}
