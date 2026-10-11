package com.loja.checkout.entrega;

import com.loja.checkout.comum.Carrinho;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal PRECO = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public boolean atende(Carrinho carrinho) {
        return carrinho.pesoKg().compareTo(PESO_MAXIMO_KG) <= 0;
    }

    @Override
    public BigDecimal frete(Carrinho carrinho) {
        return PRECO;
    }

    @Override
    public int prazoDias() {
        return 0;
    }
}
