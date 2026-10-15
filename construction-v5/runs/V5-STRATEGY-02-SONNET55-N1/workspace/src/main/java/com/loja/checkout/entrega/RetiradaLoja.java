package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Carrinho;
import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class RetiradaLoja implements Entrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public int prazoDias() {
        return 1;
    }

    @Override
    public boolean atende(Carrinho carrinho) {
        return true;
    }

    @Override
    public BigDecimal frete(Carrinho carrinho) {
        return Dinheiro.ZERO;
    }
}
