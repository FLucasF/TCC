package com.loja.checkout.entrega.impl;

import com.loja.checkout.domain.PedidoContexto;
import com.loja.checkout.entrega.ModalidadeEntrega;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EconomicaModalidade implements ModalidadeEntrega {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("12.00");
    private static final BigDecimal TAXA_POR_KG = new BigDecimal("2.00");

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public boolean disponivelPara(PedidoContexto pedido) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(PedidoContexto pedido) {
        return TAXA_FIXA.add(TAXA_POR_KG.multiply(pedido.pesoTotalKg()));
    }

    @Override
    public int prazoDias() {
        return 7;
    }
}
