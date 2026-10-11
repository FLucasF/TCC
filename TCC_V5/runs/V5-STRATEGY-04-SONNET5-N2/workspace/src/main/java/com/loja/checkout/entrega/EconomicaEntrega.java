package com.loja.checkout.entrega;

import com.loja.checkout.dinheiro.Dinheiro;
import com.loja.checkout.pedido.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EconomicaEntrega implements MetodoEntrega {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("12.00");
    private static final BigDecimal TAXA_POR_KG = new BigDecimal("2.00");

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public boolean disponivel(Pedido pedido) {
        return true;
    }

    @Override
    public BigDecimal custo(Pedido pedido) {
        return Dinheiro.arredondar(TAXA_FIXA.add(TAXA_POR_KG.multiply(pedido.pesoTotal())));
    }

    @Override
    public int prazoDias() {
        return 7;
    }
}
