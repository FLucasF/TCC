package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** R$ 12,00 + R$ 2,00 por kg do pedido, em 7 dias. */
@Component
public class EntregaEconomica implements ModalidadeEntrega {

    private static final BigDecimal FIXO = new BigDecimal("12.00");
    private static final BigDecimal POR_KG = new BigDecimal("2.00");

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public BigDecimal frete(BigDecimal pesoKgPedido) {
        return Dinheiro.centavos(FIXO.add(POR_KG.multiply(pesoKgPedido)));
    }

    @Override
    public int prazoDias() {
        return 7;
    }
}
