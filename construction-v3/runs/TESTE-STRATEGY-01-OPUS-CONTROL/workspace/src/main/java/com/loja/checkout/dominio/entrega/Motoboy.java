package com.loja.checkout.dominio.entrega;

import com.loja.checkout.comum.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** R$ 18,00 fixo, no mesmo dia, para pedidos de ate 5 kg. */
@Component
public class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public int prazoDias() {
        return 0;
    }

    @Override
    public BigDecimal calcularFrete(Pedido pedido) {
        return Dinheiro.centavos(new BigDecimal("18.00"));
    }

    @Override
    public boolean atende(Pedido pedido) {
        return pedido.pesoTotalKg().compareTo(PESO_MAXIMO_KG) <= 0;
    }
}
