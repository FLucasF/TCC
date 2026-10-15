package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Economica implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public boolean atende(Pedido pedido) {
        return true;
    }

    @Override
    public BigDecimal frete(Pedido pedido) {
        return Dinheiro.arredondar(new BigDecimal("12.00").add(pedido.pesoTotalKg().multiply(new BigDecimal("2.00"))));
    }

    @Override
    public int prazoDias(Pedido pedido) {
        return 7;
    }
}
