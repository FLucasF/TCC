package com.loja.checkout.clube;

import com.loja.checkout.service.PedidoContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Ouro implements NivelClube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.05");
    private static final BigDecimal MINIMO_PRODUTOS_BRINDE = new BigDecimal("500.00");

    @Override
    public String getCodigo() {
        return "OURO";
    }

    @Override
    public BigDecimal calcularCredito(PedidoContext pedido) {
        return pedido.subtotalProdutos().multiply(PERCENTUAL_CREDITO);
    }

    @Override
    public boolean freteGratis() {
        return true;
    }

    @Override
    public boolean temDireitoABrinde(PedidoContext pedido) {
        return pedido.subtotalProdutos().compareTo(MINIMO_PRODUTOS_BRINDE) > 0;
    }
}
