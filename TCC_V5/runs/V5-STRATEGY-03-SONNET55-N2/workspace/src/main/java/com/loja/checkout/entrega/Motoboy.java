package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public boolean atende(Pedido pedido) {
        return pedido.pesoTotalKg().compareTo(PESO_MAXIMO_KG) <= 0;
    }

    @Override
    public BigDecimal frete(Pedido pedido) {
        return Dinheiro.arredondar(new BigDecimal("18.00"));
    }

    @Override
    public int prazoDias(Pedido pedido) {
        return 0;
    }
}
