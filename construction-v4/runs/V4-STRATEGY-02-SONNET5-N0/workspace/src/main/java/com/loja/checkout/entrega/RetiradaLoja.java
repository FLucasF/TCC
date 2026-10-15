package com.loja.checkout.entrega;

import com.loja.checkout.service.PedidoContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RetiradaLoja implements OpcaoEntrega {

    @Override
    public String getCodigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public boolean disponivelPara(PedidoContext pedido) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(PedidoContext pedido) {
        return BigDecimal.ZERO;
    }

    @Override
    public int prazoDias() {
        return 1;
    }
}
