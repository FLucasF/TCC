package com.loja.checkout.entrega;

import com.loja.checkout.Dinheiro;
import com.loja.checkout.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Motoboy implements Entrega {
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public boolean atende(Pedido pedido) {
        return pedido.pesoKg().compareTo(PESO_MAXIMO_KG) <= 0;
    }

    @Override
    public BigDecimal frete(Pedido pedido) {
        return Dinheiro.arredondar(new BigDecimal("18.00"));
    }

    @Override
    public int prazoDias() {
        return 0;
    }
}
