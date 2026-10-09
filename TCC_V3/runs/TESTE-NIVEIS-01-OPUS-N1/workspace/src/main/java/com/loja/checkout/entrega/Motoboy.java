package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Carrinho;
import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** R$ 18,00 no mesmo dia, so para pedidos de ate 5 kg. */
@Component
class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");
    private static final BigDecimal VALOR = new BigDecimal("18.00");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public boolean atende(Carrinho carrinho) {
        return carrinho.pesoKg().compareTo(PESO_MAXIMO_KG) <= 0;
    }

    @Override
    public Entrega calcular(Carrinho carrinho) {
        return new Entrega(Dinheiro.centavos(VALOR), 0);
    }
}
