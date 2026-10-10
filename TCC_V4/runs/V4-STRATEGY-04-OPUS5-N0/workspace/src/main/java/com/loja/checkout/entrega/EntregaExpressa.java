package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** R$ 25,00 + R$ 4,50 por kg do pedido, em 2 dias. */
@Component
public class EntregaExpressa implements ModalidadeEntrega {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("25.00");
    private static final BigDecimal POR_KG = new BigDecimal("4.50");

    @Override
    public String codigo() {
        return "EXPRESSA";
    }

    @Override
    public int prazoEntregaDias() {
        return 2;
    }

    @Override
    public BigDecimal frete(Pedido pedido) {
        return TAXA_FIXA.add(POR_KG.multiply(pedido.pesoTotalKg()));
    }
}
