package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** R$ 12,00 + R$ 2,00 por kg, em 7 dias. */
@Component
public class EntregaEconomica implements ModalidadeEntrega {

    private static final BigDecimal FIXO = Dinheiro.de("12.00");
    private static final BigDecimal POR_KG = Dinheiro.de("2.00");

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public BigDecimal frete(Pedido pedido) {
        return Dinheiro.arredondar(FIXO.add(POR_KG.multiply(pedido.pesoKg())));
    }

    @Override
    public int prazoEntregaDias(Pedido pedido) {
        return 7;
    }
}
