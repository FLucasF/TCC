package com.loja.checkout.entrega;

import com.loja.checkout.Carrinho;
import com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Expressa implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "EXPRESSA";
    }

    @Override
    public BigDecimal frete(Carrinho carrinho) {
        return Dinheiro.centavos(new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(carrinho.pesoKg())));
    }

    @Override
    public int prazoDias() {
        return 2;
    }
}
