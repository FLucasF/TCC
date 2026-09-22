package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** R$ 25,00 + R$ 4,50 por kg, em 2 dias. */
@Component
public class EntregaExpressa implements ModalidadeEntrega {

    private static final BigDecimal FIXO = Dinheiro.de("25.00");
    private static final BigDecimal POR_KG = Dinheiro.de("4.50");

    @Override
    public String codigo() {
        return "EXPRESSA";
    }

    @Override
    public BigDecimal frete(Pedido pedido) {
        return Dinheiro.arredondar(FIXO.add(POR_KG.multiply(pedido.pesoKg())));
    }

    @Override
    public int prazoEntregaDias(Pedido pedido) {
        return 2;
    }
}
