package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** R$ 18,00 no mesmo dia, para pedidos de até 5 kg. */
@Component
class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");
    private static final BigDecimal VALOR = new BigDecimal("18.00");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public boolean atende(Pedido pedido) {
        return pedido.pesoKg().compareTo(PESO_MAXIMO_KG) <= 0;
    }

    @Override
    public Entrega apurar(Pedido pedido) {
        return new Entrega(VALOR, 0);
    }
}
