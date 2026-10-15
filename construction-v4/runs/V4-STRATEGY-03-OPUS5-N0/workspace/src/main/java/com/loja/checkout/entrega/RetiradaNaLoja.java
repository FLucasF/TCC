package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Carrinho;
import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** O cliente retira na loja: gratis, pronto em 1 dia. */
@Component
public class RetiradaNaLoja implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public int prazoDias() {
        return 1;
    }

    @Override
    public BigDecimal calcularFrete(Carrinho carrinho) {
        return Dinheiro.ZERO;
    }
}
