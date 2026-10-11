package com.loja.checkout.entrega;

import com.loja.checkout.pedido.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MotoboyEntrega implements MetodoEntrega {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public boolean disponivel(Pedido pedido) {
        return pedido.pesoTotal().compareTo(PESO_MAXIMO_KG) <= 0;
    }

    @Override
    public BigDecimal custo(Pedido pedido) {
        return TAXA_FIXA;
    }

    @Override
    public int prazoDias() {
        return 0;
    }
}
