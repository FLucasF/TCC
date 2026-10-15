package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EntregaEconomica implements ModalidadeEntrega {

    private static final BigDecimal FIXO = new BigDecimal("12.00");
    private static final BigDecimal POR_KG = new BigDecimal("2.00");

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public BigDecimal custo(Pedido pedido) {
        return Dinheiro.centavos(FIXO.add(POR_KG.multiply(pedido.pesoKg())));
    }

    @Override
    public int prazoDias() {
        return 7;
    }
}
