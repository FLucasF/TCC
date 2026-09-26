package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.PedidoContext;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class EconomicaEntrega implements ModalidadeEntrega {

    private static final BigDecimal BASE = new BigDecimal("12.00");
    private static final BigDecimal POR_KG = new BigDecimal("2.00");
    private static final int PRAZO_DIAS = 7;

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public boolean disponivel(PedidoContext pedido) {
        return true;
    }

    @Override
    public CalculoFrete calcular(PedidoContext pedido) {
        BigDecimal valor = Dinheiro.arredondar(BASE.add(POR_KG.multiply(pedido.pesoTotalKg())));
        return new CalculoFrete(valor, PRAZO_DIAS);
    }
}
