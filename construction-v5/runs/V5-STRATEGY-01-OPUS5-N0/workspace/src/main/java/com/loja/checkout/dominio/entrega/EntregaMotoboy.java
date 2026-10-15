package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** R$ 18,00 fixos, no mesmo dia, somente para pedidos de ate 5 kg. */
@Component
public class EntregaMotoboy implements ModalidadeEntrega {

    private static final BigDecimal VALOR = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public boolean atende(BigDecimal pesoKgPedido) {
        return pesoKgPedido.compareTo(PESO_MAXIMO_KG) <= 0;
    }

    @Override
    public BigDecimal frete(BigDecimal pesoKgPedido) {
        return VALOR;
    }

    @Override
    public int prazoDias() {
        return 0;
    }
}
