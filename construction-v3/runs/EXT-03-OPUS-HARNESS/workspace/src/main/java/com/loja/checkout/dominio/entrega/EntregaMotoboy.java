package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EntregaMotoboy implements ModalidadeEntrega {

    private static final BigDecimal TARIFA = new BigDecimal("18.00");
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
    public BigDecimal custo(Pedido pedido) {
        return TARIFA;
    }

    @Override
    public int prazoDias() {
        return 0;
    }
}
