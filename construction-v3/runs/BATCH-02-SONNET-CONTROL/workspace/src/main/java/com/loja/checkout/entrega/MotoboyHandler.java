package com.loja.checkout.entrega;

import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MotoboyHandler implements ModalidadeEntregaHandler {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public ModalidadeEntrega getModalidade() {
        return ModalidadeEntrega.MOTOBOY;
    }

    @Override
    public boolean disponivel(BigDecimal pesoPedidoKg) {
        return pesoPedidoKg.compareTo(PESO_MAXIMO_KG) <= 0;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoPedidoKg) {
        return Dinheiro.arredondar(TAXA_FIXA);
    }

    @Override
    public int prazoEntregaDias() {
        return 0;
    }
}
