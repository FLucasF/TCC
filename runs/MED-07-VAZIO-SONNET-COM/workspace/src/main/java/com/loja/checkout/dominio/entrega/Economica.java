package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

public class Economica implements ModalidadeEntrega {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("12.00");
    private static final BigDecimal TAXA_POR_KG = new BigDecimal("2.00");

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public boolean disponivel(Pedido pedido) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(Pedido pedido) {
        BigDecimal frete = TAXA_FIXA.add(TAXA_POR_KG.multiply(pedido.pesoTotal()));
        return Dinheiro.arredondar(frete);
    }

    @Override
    public int prazoEntregaDias() {
        return 7;
    }
}
