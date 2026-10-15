package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EntregaExpressa implements ModalidadeEntrega {

    private static final BigDecimal FIXO = Dinheiro.reais("25.00");
    private static final BigDecimal POR_KG = Dinheiro.reais("4.50");
    private static final int PRAZO_DIAS = 2;

    @Override
    public String codigo() {
        return "EXPRESSA";
    }

    @Override
    public boolean atende(Pedido pedido) {
        return true;
    }

    @Override
    public Entrega calcular(Pedido pedido) {
        BigDecimal frete = FIXO.add(POR_KG.multiply(pedido.pesoTotalKg()));
        return new Entrega(Dinheiro.emCentavos(frete), PRAZO_DIAS);
    }
}
