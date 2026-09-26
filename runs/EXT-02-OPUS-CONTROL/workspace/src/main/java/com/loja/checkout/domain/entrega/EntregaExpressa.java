package com.loja.checkout.domain.entrega;

import com.loja.checkout.domain.pedido.Dinheiro;
import com.loja.checkout.domain.pedido.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** R$ 25,00 + R$ 4,50 por kg, em 2 dias. */
@Component
public class EntregaExpressa implements ModalidadeEntrega {

    private static final BigDecimal FIXO = new BigDecimal("25.00");
    private static final BigDecimal POR_KG = new BigDecimal("4.50");

    @Override
    public String codigo() {
        return "EXPRESSA";
    }

    @Override
    public BigDecimal frete(Pedido pedido) {
        return Dinheiro.valor(FIXO.add(POR_KG.multiply(pedido.pesoTotalKg())));
    }

    @Override
    public int prazoEntregaDias(Pedido pedido) {
        return 2;
    }
}
