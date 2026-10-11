package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Motoboy implements OpcaoEntrega {

    private static final BigDecimal CUSTO_FIXO = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public int prazoDias() {
        return 0;
    }

    @Override
    public boolean disponivel(Pedido pedido) {
        return pedido.pesoTotal().compareTo(PESO_MAXIMO_KG) <= 0;
    }

    @Override
    public BigDecimal custo(Pedido pedido) {
        return CUSTO_FIXO;
    }
}
