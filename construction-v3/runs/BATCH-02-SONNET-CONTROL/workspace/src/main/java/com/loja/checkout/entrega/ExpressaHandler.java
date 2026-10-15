package com.loja.checkout.entrega;

import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ExpressaHandler implements ModalidadeEntregaHandler {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("25.00");
    private static final BigDecimal TAXA_POR_KG = new BigDecimal("4.50");

    @Override
    public ModalidadeEntrega getModalidade() {
        return ModalidadeEntrega.EXPRESSA;
    }

    @Override
    public boolean disponivel(BigDecimal pesoPedidoKg) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoPedidoKg) {
        return Dinheiro.arredondar(TAXA_FIXA.add(TAXA_POR_KG.multiply(pesoPedidoKg)));
    }

    @Override
    public int prazoEntregaDias() {
        return 2;
    }
}
