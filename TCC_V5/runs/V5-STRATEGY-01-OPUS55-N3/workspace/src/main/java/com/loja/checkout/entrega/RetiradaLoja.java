package com.loja.checkout.entrega;

import com.loja.checkout.comum.Carrinho;
import com.loja.checkout.comum.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public boolean atende(Carrinho carrinho) {
        return true;
    }

    @Override
    public BigDecimal frete(Carrinho carrinho) {
        return Dinheiro.ZERO;
    }

    @Override
    public int prazoDias() {
        return 1;
    }
}
