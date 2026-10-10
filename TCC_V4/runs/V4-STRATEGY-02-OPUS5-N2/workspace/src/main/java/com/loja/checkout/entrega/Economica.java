package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Economica implements ModalidadeEntrega {

    private static final BigDecimal PARCELA_FIXA = new BigDecimal("12.00");
    private static final BigDecimal POR_KG = new BigDecimal("2.00");

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public BigDecimal frete(Pedido pedido) {
        return Dinheiro.arredondar(PARCELA_FIXA.add(POR_KG.multiply(pedido.pesoTotalKg())));
    }

    @Override
    public int prazoDias() {
        return 7;
    }
}
