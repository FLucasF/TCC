package com.loja.checkout.entrega;

import com.loja.checkout.dominio.PedidoContext;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class MotoboyEntrega implements ModalidadeEntrega {

    private static final BigDecimal VALOR = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");
    private static final int PRAZO_DIAS = 0;

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public boolean disponivel(PedidoContext pedido) {
        return pedido.pesoTotalKg().compareTo(PESO_MAXIMO_KG) <= 0;
    }

    @Override
    public CalculoFrete calcular(PedidoContext pedido) {
        return new CalculoFrete(VALOR, PRAZO_DIAS);
    }
}
