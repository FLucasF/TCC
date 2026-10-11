package com.loja.checkout.entrega;

import com.loja.checkout.model.Pedido;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component("ECONOMICA")
public class Economica implements ModalidadeEntrega {
    private static final BigDecimal TAXA_FIXA = new BigDecimal("12.00");
    private static final BigDecimal TAXA_POR_KG = new BigDecimal("2.00");

    @Override
    public BigDecimal calcularFrete(Pedido pedido) {
        BigDecimal valorPorPeso = TAXA_POR_KG.multiply(pedido.pesoTotal());
        return TAXA_FIXA.add(valorPorPeso).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public Integer prazoEntregaDias() {
        return 7;
    }

    @Override
    public void validarDisponibilidade(Pedido pedido) {
    }
}
