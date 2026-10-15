package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Economica implements OpcaoEntrega {

    private static final BigDecimal CUSTO_FIXO = new BigDecimal("12.00");
    private static final BigDecimal CUSTO_POR_KG = new BigDecimal("2.00");

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public int prazoDias() {
        return 7;
    }

    @Override
    public boolean disponivel(Pedido pedido) {
        return true;
    }

    @Override
    public BigDecimal custo(Pedido pedido) {
        return CUSTO_FIXO.add(CUSTO_POR_KG.multiply(pedido.pesoTotal()));
    }
}
