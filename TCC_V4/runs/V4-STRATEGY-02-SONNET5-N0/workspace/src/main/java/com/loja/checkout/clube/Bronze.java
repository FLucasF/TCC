package com.loja.checkout.clube;

import com.loja.checkout.service.PedidoContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Bronze implements NivelClube {

    @Override
    public String getCodigo() {
        return "BRONZE";
    }

    @Override
    public BigDecimal calcularCredito(PedidoContext pedido) {
        return BigDecimal.ZERO;
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
