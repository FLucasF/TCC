package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** R$ 12,00 mais R$ 2,00 por quilo, em 7 dias. */
@Component
public class Economica implements ModalidadeEntrega {

    private static final BigDecimal FIXO = new BigDecimal("12.00");
    private static final BigDecimal POR_KG = new BigDecimal("2.00");
    private static final int PRAZO_DIAS = 7;

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public Entrega calcular(Pedido pedido) {
        BigDecimal valor = FIXO.add(POR_KG.multiply(pedido.pesoKg()));
        return new Entrega(Dinheiro.centavos(valor), PRAZO_DIAS);
    }
}
