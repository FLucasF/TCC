package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** R$ 18,00 no mesmo dia, so para pedidos de ate 5 kg. */
@Component
public class EntregaMotoboy implements ModalidadeEntrega {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public int prazoEntregaDias() {
        return 0;
    }

    @Override
    public BigDecimal frete(Pedido pedido) {
        return TAXA_FIXA;
    }

    @Override
    public boolean atende(Pedido pedido) {
        return pedido.pesoTotalKg().compareTo(PESO_MAXIMO_KG) <= 0;
    }
}
