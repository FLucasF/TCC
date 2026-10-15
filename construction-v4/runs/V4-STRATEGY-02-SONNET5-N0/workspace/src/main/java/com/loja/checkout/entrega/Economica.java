package com.loja.checkout.entrega;

import com.loja.checkout.service.PedidoContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Economica implements OpcaoEntrega {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("12.00");
    private static final BigDecimal TAXA_POR_KG = new BigDecimal("2.00");

    @Override
    public String getCodigo() {
        return "ECONOMICA";
    }

    @Override
    public boolean disponivelPara(PedidoContext pedido) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(PedidoContext pedido) {
        return TAXA_FIXA.add(TAXA_POR_KG.multiply(pedido.pesoTotalKg()));
    }

    @Override
    public int prazoDias() {
        return 7;
    }
}
