package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** R$ 25,00 mais R$ 4,50 por quilo, em 2 dias. */
@Component
public class Expressa implements ModalidadeEntrega {

    private static final BigDecimal FIXO = new BigDecimal("25.00");
    private static final BigDecimal POR_KG = new BigDecimal("4.50");
    private static final int PRAZO_DIAS = 2;

    @Override
    public String codigo() {
        return "EXPRESSA";
    }

    @Override
    public Entrega calcular(Pedido pedido) {
        BigDecimal valor = FIXO.add(POR_KG.multiply(pedido.pesoKg()));
        return new Entrega(Dinheiro.centavos(valor), PRAZO_DIAS);
    }
}
