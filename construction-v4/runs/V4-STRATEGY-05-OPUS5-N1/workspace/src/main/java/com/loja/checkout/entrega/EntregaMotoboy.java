package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EntregaMotoboy implements ModalidadeEntrega {

    private static final BigDecimal FIXO = Dinheiro.reais("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");
    private static final int PRAZO_DIAS = 0;

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public boolean atende(Pedido pedido) {
        return pedido.pesoTotalKg().compareTo(PESO_MAXIMO_KG) <= 0;
    }

    @Override
    public Entrega calcular(Pedido pedido) {
        return new Entrega(FIXO, PRAZO_DIAS);
    }
}
