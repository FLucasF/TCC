package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Entrega;
import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** R$ 12,00 + R$ 2,00 por kg, em 7 dias. */
@Component
public class Economica implements ModalidadeEntrega {

    private static final BigDecimal BASE = new BigDecimal("12.00");
    private static final BigDecimal POR_KG = new BigDecimal("2.00");
    private static final int PRAZO_DIAS = 7;

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public Entrega calcular(Pedido pedido) {
        return new Entrega(Dinheiro.arredondar(BASE.add(POR_KG.multiply(pedido.pesoKg()))), PRAZO_DIAS);
    }
}
