package com.loja.checkout.entrega;

import com.loja.checkout.model.Pedido;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component("EXPRESSA")
public class Expressa implements ModalidadeEntrega {
    private static final BigDecimal TAXA_FIXA = new BigDecimal("25.00");
    private static final BigDecimal TAXA_POR_KG = new BigDecimal("4.50");

    @Override
    public BigDecimal calcularFrete(Pedido pedido) {
        BigDecimal valorPorPeso = TAXA_POR_KG.multiply(pedido.pesoTotal());
        return TAXA_FIXA.add(valorPorPeso).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public Integer prazoEntregaDias() {
        return 2;
    }

    @Override
    public void validarDisponibilidade(Pedido pedido) {
    }
}
