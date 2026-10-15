package com.loja.checkout.entrega;

import com.loja.checkout.comum.Dinheiro;
import com.loja.checkout.pedido.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** R$ 18,00 fixos, no mesmo dia, para pedidos de ate 5 kg. */
@Component
public class EntregaMotoboy implements ModalidadeEntrega {

    private static final BigDecimal VALOR = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public int prazoEntregaDias() {
        return 0;
    }

    @Override
    public BigDecimal calcularFrete(Pedido pedido) {
        return Dinheiro.arredondar(VALOR);
    }

    @Override
    public boolean atende(Pedido pedido) {
        return pedido.pesoTotalKg().compareTo(PESO_MAXIMO_KG) <= 0;
    }
}
