package br.com.loja.checkout.entrega;

import br.com.loja.checkout.Dinheiro;
import br.com.loja.checkout.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
class Motoboy implements Entrega {

    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public boolean atende(Pedido pedido) {
        return pedido.pesoKg().compareTo(PESO_MAXIMO_KG) <= 0;
    }

    @Override
    public BigDecimal frete(Pedido pedido) {
        return Dinheiro.de("18.00");
    }

    @Override
    public int prazoDias() {
        return 0;
    }
}
