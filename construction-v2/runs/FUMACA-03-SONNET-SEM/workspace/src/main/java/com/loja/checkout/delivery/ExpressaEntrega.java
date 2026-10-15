package com.loja.checkout.delivery;

import com.loja.checkout.PedidoContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ExpressaEntrega implements ModalidadeEntregaStrategy {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("25.00");
    private static final BigDecimal TAXA_POR_KG = new BigDecimal("4.50");

    @Override
    public String getCodigo() {
        return "EXPRESSA";
    }

    @Override
    public int getPrazoDias() {
        return 2;
    }

    @Override
    public boolean disponivelPara(PedidoContext pedido) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(PedidoContext pedido) {
        return TAXA_FIXA.add(TAXA_POR_KG.multiply(pedido.pesoTotalKg()));
    }
}
