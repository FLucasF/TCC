package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Expressa implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "EXPRESSA";
    }

    @Override
    public boolean atende(Pedido pedido) {
        return true;
    }

    @Override
    public BigDecimal frete(Pedido pedido) {
        return Dinheiro.arredondar(new BigDecimal("25.00").add(pedido.pesoTotalKg().multiply(new BigDecimal("4.50"))));
    }

    @Override
    public int prazoDias(Pedido pedido) {
        return 2;
    }
}
