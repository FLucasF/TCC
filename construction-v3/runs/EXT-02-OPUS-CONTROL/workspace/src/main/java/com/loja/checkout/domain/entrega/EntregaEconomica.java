package com.loja.checkout.domain.entrega;

import com.loja.checkout.domain.pedido.Dinheiro;
import com.loja.checkout.domain.pedido.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** R$ 12,00 + R$ 2,00 por kg, em 7 dias. */
@Component
public class EntregaEconomica implements ModalidadeEntrega {

    private static final BigDecimal FIXO = new BigDecimal("12.00");
    private static final BigDecimal POR_KG = new BigDecimal("2.00");

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public BigDecimal frete(Pedido pedido) {
        return Dinheiro.valor(FIXO.add(POR_KG.multiply(pedido.pesoTotalKg())));
    }

    @Override
    public int prazoEntregaDias(Pedido pedido) {
        return 7;
    }
}
